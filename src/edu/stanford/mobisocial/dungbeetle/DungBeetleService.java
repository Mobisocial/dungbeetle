package edu.stanford.mobisocial.dungbeetle;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private MessagingManagerThread mManagerThread;
    private DBHelper mHelper;
    public static final String TAG = "DungBeetleService";

	private Handler mToastHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(DungBeetleService.this,
                               msg.obj.toString(),
                               Toast.LENGTH_SHORT).show();
            }
        };

	private Handler mMessageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                IncomingMessage obj = (IncomingMessage)msg.obj;
                handleIncomingDirectMessage(obj);
            }
        };

    private List<MessageHandler> mHandlers = new ArrayList<MessageHandler>();

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
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
            launch.putExtra("creator", false);
            launch.setPackage(packageName);
            final PackageManager mgr = getPackageManager();
            List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
            if (resolved.size() == 0) {
                Toast.makeText(DungBeetleService.this, 
                               "Could not find application to handle invite", 
                               Toast.LENGTH_SHORT).show();
                return;
            }
            ActivityInfo info = resolved.get(0).activityInfo;
            launch.setComponent(new ComponentName(
                                    info.packageName,
                                    info.name));
            Notification notification = new Notification(
                R.drawable.icon, "New Invitation", System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(
                DungBeetleService.this, 0, launch, 0);
            notification.setLatestEventInfo(
                DungBeetleService.this, 
                "Invitation received from " + from.email, 
                "Click to launch application.", 
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
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
                long[] idArray = new long[ids.length()];
                for(int i = 0; i < ids.length(); i++) {
                    Log.i(TAG, "Passing off " + ids.getLong(i));
                    idArray[i] = ids.getLong(i);
                }
                launch.putExtra("participants", idArray);
                launch.setPackage(packageName);
                final PackageManager mgr = getPackageManager();
                List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
                if (resolved.size() == 0) {
                    Toast.makeText(
                        DungBeetleService.this, 
                        "Could not find application to handle invite.", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                ActivityInfo info = resolved.get(0).activityInfo;
                launch.setComponent(new ComponentName(
                                        info.packageName,
                                        info.name));
                Notification notification = new Notification(
                    R.drawable.icon, "New Invitation from " + from.email, 
                    System.currentTimeMillis());
                PendingIntent contentIntent = PendingIntent.getActivity(
                    DungBeetleService.this, 0, launch, 0);
                notification.setLatestEventInfo(
                    DungBeetleService.this, 
                    "Invitation received from " + from.email, 
                    "Click to launch application: " + packageName, 
                    contentIntent);
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                mNotificationManager.notify(0, notification);
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
        	String webUrl = obj.optString(InviteObj.WEB_URL);
            launch.setData(Uri.parse(webUrl));

            Notification notification = new Notification(
                R.drawable.icon, "New Invitation", System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(
                DungBeetleService.this, 0, launch, 0);
            notification.setLatestEventInfo(
                DungBeetleService.this, "Invitation received", 
                "Click to launch application.", 
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
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
            Notification notification = new Notification(
                R.drawable.icon, "New Shared File...", System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(
                DungBeetleService.this, 0, i, 0);
            notification.setLatestEventInfo(
                DungBeetleService.this, "New Shared File",
                mimeType + "  " + uri,
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
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
                                    getPackageName(),
                                    DungBeetleActivity.class.getName()));
            Notification notification = new Notification(
                R.drawable.icon, "IM from " + from.email, System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(
                DungBeetleService.this, 0, launch, 0);

            String msg = obj.optString("text");

            notification.setLatestEventInfo(
                DungBeetleService.this, "IM from " + from.email,
                "\"" + msg + "\"", contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
        }
    }


    class SubscribeReqHandler extends MessageHandler{
        boolean willHandle(Contact from, JSONObject msg){ 
            return msg.optString("type").equals("subscribe_req");
        }
        void handle(Contact from, JSONObject obj){
            Helpers.insertSubscriber(
                DungBeetleService.this, 
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
            mHelper.setContactName(id, name);
        }
    }


    private void handleIncomingDirectMessage(IncomingMessage incoming){
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


    @Override
    public void onCreate() {
        mHelper = new DBHelper(this);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mManagerThread = new MessagingManagerThread(this, mToastHandler, mMessageHandler);
        mManagerThread.start();
        mHandlers.add(new SubscribeReqHandler());
        mHandlers.add(new IMHandler());
        mHandlers.add(new InviteToWebSessionHandler());
        mHandlers.add(new InviteToSharedAppHandler());
        mHandlers.add(new InviteToSharedAppFeedHandler());
        mHandlers.add(new SendFileHandler());
        mHandlers.add(new ProfileHandler());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DungBeetleService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        mNotificationManager.cancel(R.string.active);
        Toast.makeText(this, R.string.stopping, Toast.LENGTH_SHORT).show();
        mHelper.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

// This is the object that receives interactions from clients.  See
// RemoteService for a more complete example.
    private final IBinder mBinder = new Binder(){
            DungBeetleService getService(){
                return DungBeetleService.this;
            }
        };


    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        CharSequence text = getText(R.string.start);
        Notification notification = new Notification(
            R.drawable.icon, 
            text,
            System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(
            this, 0,
            new Intent(this, DungBeetleActivity.class), 0);

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                                        text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNotificationManager.notify(R.string.active, notification);
    }
}
