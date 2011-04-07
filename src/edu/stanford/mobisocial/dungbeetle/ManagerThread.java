package edu.stanford.mobisocial.dungbeetle;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.bumblebee.MessageListener;
import edu.stanford.mobisocial.bumblebee.MessengerService;
import edu.stanford.mobisocial.bumblebee.OutgoingMessage;
import edu.stanford.mobisocial.bumblebee.StateListener;
import edu.stanford.mobisocial.bumblebee.TransportIdentityProvider;
import edu.stanford.mobisocial.bumblebee.XMPPMessengerService;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.StringSearchAndReplacer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import org.json.JSONObject;

public class ManagerThread extends Thread {
    public static final String TAG = "ManagerThread";
    private Handler mToastHandler;
    private Handler mDirectMessageHandler;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    public ManagerThread(final Context context, 
                         final Handler toastHandler, 
                         final Handler directMessageHandler){
        mToastHandler = toastHandler;
        mDirectMessageHandler = directMessageHandler;
        mContext = context;
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
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Connected to message transport!";
                    mToastHandler.sendMessage(m);
                }
                public void onNotReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Message transport not available.";
                    mToastHandler.sendMessage(m);
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage incoming) {
                    Log.i(TAG, "Got incoming message " + incoming);
                    handleIncomingMessage(incoming);
                }
            });

        mOco = new ObjectContentObserver(new Handler(mContext.getMainLooper()));

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds"), true, mOco);

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out"), true, mOco);
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
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName), null);
                if(feedName.equals("direct") || feedName.equals("friend")){
                    Message m = mDirectMessageHandler.obtainMessage();
                    m.obj = localizedMsg;
                    mDirectMessageHandler.sendMessage(m);
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
        public synchronized void waitForChange() {
            if(changed)
                return;
            try {
                wait();
                changed = false;
            } catch(InterruptedException e) {}
        }
        public synchronized void clearChanged() {
            changed = false;
        }
    };


}
