package edu.stanford.mobisocial.dungbeetle;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.bumblebee.MessageListener;
import edu.stanford.mobisocial.bumblebee.MessengerService;
import edu.stanford.mobisocial.bumblebee.OutgoingMessage;
import edu.stanford.mobisocial.bumblebee.StateListener;
import edu.stanford.mobisocial.bumblebee.TransportIdentityProvider;
import edu.stanford.mobisocial.bumblebee.XMPPMessengerService;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.StringSearchAndReplacer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;

public class MessagingManagerThread extends Thread {
    public static final String TAG = "MessagingManagerThread";
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private Handler mMainThreadHandler;
	private NotificationManager mNotificationManager;
	private PresenceAwareNotify presenceAwareNotify = new PresenceAwareNotify();
	private int notifyCounter = 0;
    private final long[] VIBRATE = new long[] {0, 280, 150, 100, 150, 150, 50, 250};


    public MessagingManagerThread(final Context context){
        mNotificationManager = (NotificationManager)
            context.getSystemService(context.NOTIFICATION_SERVICE);
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
        ConnectionStatus status = new ConnectionStatus(){
                public boolean isConnected(){
                    ConnectivityManager cm = 
                        (ConnectivityManager)context.getSystemService(
                            Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = cm.getActiveNetworkInfo();
                    return info != null && info.isConnected();
                }
            };
		mMessenger = new XMPPMessengerService(wrapIdent(mIdent), status);
		mMessenger.addStateListener(new StateListener() {
                public void onReady() {
                    Log.i(TAG, "Connected to message transport!");
                }
                public void onNotReady() {
                    Log.i(TAG, "Message transport not available.");
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage incoming) {
                    Log.i(TAG, "Got incoming message " + incoming);
                    handleIncomingMessage(incoming);
                }
            });

        mHandlers.add(new SubscribeReqHandler());
        mHandlers.add(new IMHandler());
        mHandlers.add(new InviteToWebSessionHandler());
        mHandlers.add(new InviteToSharedAppHandler());
        mHandlers.add(new InviteToSharedAppFeedHandler());
        mHandlers.add(new SendFileHandler());
        mHandlers.add(new ProfileHandler());
        mHandlers.add(new PresenceHandler());
        mHandlers.add(new StatusHandler());

        mOco = new ObjectContentObserver(new Handler(mContext.getMainLooper()));

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds"), true, mOco);

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out"), true, mOco);
    }

    private void toastInMainThread(final String msg){
        mMainThreadHandler.post(new Runnable(){
                public void run(){
                    Toast.makeText(mContext, msg,Toast.LENGTH_SHORT).show();
                }});
    }

    private int nextNotifyId(){
        notifyCounter += 1;
        return notifyCounter;
    }


    // FYI: Invoked on XMPP reader thread
    private void handleIncomingMessage(final IncomingMessage incoming){
        final String personId = incoming.from();
        final String contents = localize(incoming.contents());
        final IncomingMessage localizedMsg = new IncomingMessage(){
                public String contents(){ return contents; }
                public String from(){ return personId; }
            };
        Log.i(TAG, "Localized contents: " + contents);
        try{
            JSONObject obj = new JSONObject(contents);
            String feedName = obj.getString("feedName");
            Maybe<Contact> contact = mHelper.contactForPersonId(personId);
            if(contact.isKnown()){
                mHelper.addObjectByJson(contact.otherwise(Contact.NA()).id, obj);
                mContext.getContentResolver().notifyChange(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                              "/feeds/" + feedName), null);
                if(feedName.equals("direct") || feedName.equals("friend")){
                    mMainThreadHandler.post(new Runnable(){
                            public void run(){
                                handleSpecialMessage(localizedMsg);
                            }
                        });
                }
            }
            else{
                Log.i(TAG, "Message from unknown contact. " + contents);
            }

        }
        catch(Exception e){
            Log.e(TAG, "Error handling incoming message: " + e.toString());
        }
    }


    /**
     * Replace global contact and object references 
     * with their local analogs.
     */
    private String localize(String s){
        return mContactLocalizer.apply(s);
    }


    /**
     * Replace local contact and object ids 
     * with their global analogs.
     */
    private String globalize(String s){
        return mContactGlobalizer.apply(s);
    }


    // Note, doing a query on each replacement is not friendly : (
    // Should use a contact cache here.

    private StringSearchAndReplacer mContactGlobalizer = new StringSearchAndReplacer("\"@l([\\-0-9]+)\""){
            protected String replace(Matcher m){
                Long id = Long.valueOf(m.group(1));
                String personId;
                if(id.equals(Contact.MY_ID)){
                    personId = mIdent.userPersonId();
                }
                else{
                    Maybe<Contact> c = mHelper.contactForContactId(id);
                    personId = c.otherwise(Contact.NA()).personId;
                }
                return "\"@g" + personId + "\"";
            }
        };

    private StringSearchAndReplacer mContactLocalizer = new StringSearchAndReplacer("\"@g([^\"]+)\""){
            protected String replace(Matcher m){
                String personId = m.group(1);
                Maybe<Contact> c = mHelper.contactForPersonId(personId);
                return String.valueOf(c.otherwise(Contact.NA()).id);
            }
        };


    @Override
    public void run(){
        Log.i(TAG, "Starting DungBeetle manager thread");
        Log.i(TAG, "Starting messenger...");
        mMessenger.init();
        while(!interrupted()) {
            try{
                if(mOco.changed){
                    Log.i(TAG, "Noticed change...");
                    mOco.clearChanged();
                    Cursor objs = mHelper.queryUnsentObjects();
                    Log.i(TAG, objs.getCount() + " objects...");
                    objs.moveToFirst();
                    ArrayList<Long> sent = new ArrayList<Long>();
                    while(!objs.isAfterLast()){
                        String to = objs.getString(objs.getColumnIndexOrThrow(Object.DESTINATION));
                        if(to != null){
                            OutgoingMessage m = new OutgoingDirectObjectMsg(objs);
                            Log.i(TAG, "Sending direct message " + m);
                            if(m.toPublicKeys().isEmpty()){
                                Log.e(TAG, "Empty addressees!");
                            }
                            mMessenger.sendMessage(m);
                        }
                        else{
                            OutgoingMessage m = new OutgoingFeedObjectMsg(objs);
                            Log.i(TAG, "Sending feed object " + m);
                            if(m.toPublicKeys().isEmpty()){
                                Log.e(TAG, "Empty addressees!");
                            }
                            mMessenger.sendMessage(m);
                        }
                        sent.add(objs.getLong(objs.getColumnIndexOrThrow(Object._ID)));
                        objs.moveToNext();
                    }
                    mHelper.markObjectsAsSent(sent);
                }
            }
            catch(Exception e){
                Log.wtf(TAG, e);
            }

            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {}
        }
        mHelper.close();
    }

    private abstract class OutgoingMsg implements OutgoingMessage{
        protected String mBody;
        protected List<PublicKey> mPubKeys;
        public List<PublicKey> toPublicKeys(){ return mPubKeys; }
        public String contents(){ return mBody; }
        public String toString(){ return "[Message with body: " + mBody + " to " + toPublicKeys().size() + " recipient(s) ]"; }
    }

    private class OutgoingFeedObjectMsg extends OutgoingMsg{
        public OutgoingFeedObjectMsg(Cursor objs){
            String feedName = objs.getString(
                objs.getColumnIndexOrThrow(Object.FEED_NAME));
            Cursor subs = mHelper.querySubscribers(feedName);
            subs.moveToFirst();
            ArrayList<Long> ids = new ArrayList<Long>();
            while(!subs.isAfterLast()){
                ids.add(subs.getLong(
                            subs.getColumnIndexOrThrow(Subscriber.CONTACT_ID)));
                subs.moveToNext();
            }
            mPubKeys = mIdent.publicKeysForContactIds(ids);
            mBody = globalize(objs.getString(objs.getColumnIndexOrThrow(Object.JSON)));
        }
    }

    private class OutgoingDirectObjectMsg extends OutgoingMsg{
        public OutgoingDirectObjectMsg(Cursor objs){
            String to = objs.getString(
                objs.getColumnIndexOrThrow(Object.DESTINATION));
            String[] tos = to.split(",");
            Long[] ids = new Long[tos.length];
            for(int i = 0; i < tos.length; i++) ids[i] = Long.valueOf(tos[i]);
            mPubKeys = mIdent.publicKeysForContactIds(Arrays.asList(ids));
            mBody = globalize(objs.getString(objs.getColumnIndexOrThrow(Object.JSON)));
        }
    }

    private TransportIdentityProvider wrapIdent(final IdentityProvider ident){
        return new TransportIdentityProvider(){
            public PublicKey userPublicKey(){
                return ident.userPublicKey();
            }
            public PrivateKey userPrivateKey(){
                return ident.userPrivateKey();
            }
            public String userPersonId(){
                return ident.userPersonId();
            }
            public PublicKey publicKeyForPersonId(String id){
                return ident.publicKeyForPersonId(id);
            }
            public String personIdForPublicKey(PublicKey key){
                return ident.personIdForPublicKey(key);
            }
        };
    }



    class ObjectContentObserver extends ContentObserver {
        public boolean changed;
        public ObjectContentObserver(Handler h)  {
            super(h);
            // Default to true so we do the initial check.
            changed = true; 
        }
        @Override
        public synchronized void onChange(boolean self) {
            changed = true;
            notify();
        }
        public synchronized void clearChanged() {
            changed = false;
        }
    };

    private List<MessageHandler> mHandlers = new ArrayList<MessageHandler>();

    // FYI: Must be invoked from main app thread. See above.
    private void handleSpecialMessage(IncomingMessage incoming){
        String contents = incoming.contents();
        final Maybe<Contact> c = mHelper.contactForPersonId(incoming.from());
        try{
            JSONObject obj = new JSONObject(contents);
            for(MessageHandler h : mHandlers){
                if(h.willHandle(c.otherwise(Contact.NA()), obj)){
                    h.handle(c.otherwise(Contact.NA()), obj);
                    break;
                }
            }
        }
        catch(JSONException e){ throw new RuntimeException(e); }
    }


    abstract class MessageHandler{
        abstract boolean willHandle(Contact from, JSONObject msg);
        abstract void handle(Contact from, JSONObject msg);
    }

    class InviteToSharedAppHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("invite_app_session");
        }
        void handle(Contact from, JSONObject obj){
            String packageName = obj.optString(InviteObj.PACKAGE_NAME);
            String arg = obj.optString(InviteObj.ARG);
            Log.i(TAG, "Received invite with arg: " + arg);
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
            launch.putExtra("creator", false);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launch.setPackage(packageName);
            final PackageManager mgr = mContext.getPackageManager();
            List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
            if (resolved == null || resolved.size() == 0) {
                Toast.makeText(mContext, 
                               "Could not find application to handle invite", 
                               Toast.LENGTH_SHORT).show();
                return;
            }
            ActivityInfo info = resolved.get(0).activityInfo;
            launch.setComponent(new ComponentName(
                                    info.packageName,
                                    info.name));
            PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

            presenceAwareNotify.notify(
                "New Invitation",
                "Invitation received from " + from.name, 
                "Click to launch application.", 
                contentIntent);
        }
    }

    class InviteToSharedAppFeedHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("invite_app_feed");
        }
        void handle(Contact from, JSONObject obj){
            try{
                String packageName = obj.getString(InviteObj.PACKAGE_NAME);
                String feedName = obj.getString("sharedFeedName");
                JSONArray ids = obj.getJSONArray(InviteObj.PARTICIPANTS);
                Intent launch = new Intent();
                launch.setAction(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.putExtra("type", "invite_app_feed");
                launch.putExtra("creator", false);
                launch.putExtra("sender", from.id);
                launch.putExtra("sharedFeedName", feedName);
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                long[] idArray = new long[ids.length()];
                for(int i = 0; i < ids.length(); i++) {
                    Log.i(TAG, "Passing off " + ids.getLong(i));
                    idArray[i] = ids.getLong(i);
                }
                launch.putExtra("participants", idArray);
                launch.setPackage(packageName);
                final PackageManager mgr = mContext.getPackageManager();
                List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
                if (resolved.size() == 0) {
                    Toast.makeText(
                        mContext, 
                        "Could not find application to handle invite.", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                ActivityInfo info = resolved.get(0).activityInfo;
                launch.setComponent(new ComponentName(
                                        info.packageName,
                                        info.name));
                PendingIntent contentIntent = PendingIntent.getActivity(
                    mContext, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

                presenceAwareNotify.notify(
                    "New Invitation from " + from.name, 
                    "Invitation received from " + from.name, 
                    "Click to launch application: " + packageName, 
                    contentIntent);
            }
            catch(JSONException e){
                Log.e(TAG, e.getMessage());
            }
        }
    }

    class InviteToWebSessionHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("invite_web_session");
        }
        void handle(Contact from, JSONObject obj){

            String arg = obj.optString(InviteObj.ARG);
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
            launch.putExtra("creator", false);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	String webUrl = obj.optString(InviteObj.WEB_URL);
            launch.setData(Uri.parse(webUrl));

            PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

            presenceAwareNotify.notify(
                "New Invitation",
                "Invitation received", 
                "Click to launch application.", 
                contentIntent);
        }
    }


    class SendFileHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("send_file");
        }
        void handle(Contact from, JSONObject obj){
            String mimeType = obj.optString("mimeType");
            String uri = obj.optString("uri");
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setType(mimeType);
            i.setData(Uri.parse(uri));
            i.putExtra(Intent.EXTRA_TEXT, uri);

            PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

            presenceAwareNotify.notify(
                "New Shared File...",
                "New Shared File",
                mimeType + "  " + uri,
                contentIntent);
        }
    }


    class IMHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("instant_message");
        }
        void handle(Contact from, JSONObject obj){
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(
                                    mContext.getPackageName(),
                                    DungBeetleActivity.class.getName()));
            PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

            String msg = obj.optString("text");

            presenceAwareNotify.notify(
                "IM from " + from.name,
                "IM from " + from.name,
                "\"" + msg + "\"", 
                contentIntent);
        }
    }


    class SubscribeReqHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("subscribe_req");
        }
        void handle(Contact from, JSONObject obj){
            Helpers.insertSubscriber(
                mContext, 
                from.id,
                obj.optString("subscribeToFeed"));
        }
    }

    class ProfileHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){
            return msg.optString("type").equals("profile");
        }
        void handle(Contact from, JSONObject obj){
            String name = obj.optString("name");
            String id = Long.toString(from.id);
            
                    Log.i(TAG, "Updating " + id + " name="+name);
            ContentValues values = new ContentValues();
            values.put(Contact.NAME, name);
            mContext.getContentResolver().update(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), values, "_id=?", new String[]{id});
        }
    }

    class PresenceHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){
            return msg.optString("type").equals("presence");
        }
        void handle(Contact from, JSONObject obj){
            int presence = Integer.parseInt(obj.optString("presence"));
            String id = Long.toString(from.id);
            
            ContentValues values = new ContentValues();
            values.put(Contact.PRESENCE, presence);
            mContext.getContentResolver().update(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), values, "_id=?", new String[]{id});
        }
    }

    //figure out what to name this stuff
    class StatusHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){
            return msg.optString("type").equals("status");
        }
        void handle(Contact from, JSONObject obj){
            String status = obj.optString("status");
            String id = Long.toString(from.id);
            
            ContentValues values = new ContentValues();
            values.put(Contact.STATUS, status);
            mContext.getContentResolver().update(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), values, "_id=?", new String[]{id});
        }
    }

    private class PresenceAwareNotify {

        public PresenceAwareNotify() {

        }
        
        public void notify(String notificationTitle, String notificationMsg, String notificationSubMsg, PendingIntent contentIntent) {
            Notification notification = new Notification(
                R.drawable.icon, notificationTitle, System.currentTimeMillis());

            Cursor c = mContext.getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
                null, 
                Object.TYPE + "=?", 
                new String[]{ "presence"}, 
                Object.TIMESTAMP + " DESC");

            if(c.moveToFirst()) {
                String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    int myPresence = Integer.parseInt(obj.optString("presence"));
                    if(myPresence == Presence.AVAILABLE || myPresence == Presence.AWAY) {
                        notification.vibrate = VIBRATE;
                    }
                }catch(JSONException e){}
            }
            notification.setLatestEventInfo(
                mContext, 
                notificationMsg, 
                notificationSubMsg, 
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(nextNotifyId(), notification);
        }
    }

}
