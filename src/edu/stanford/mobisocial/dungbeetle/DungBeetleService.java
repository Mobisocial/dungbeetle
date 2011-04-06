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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;


public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private ManagerThread mManagerThread;
    private DBIdentityProvider mIdent;

	private Handler mToastHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(DungBeetleService.this,
                               msg.obj.toString(),
                               Toast.LENGTH_SHORT).show();
            }
        };

	private Handler mDirectMessageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                IncomingMessage obj = (IncomingMessage)msg.obj;
                handleIncomingDirectMessage(obj);
            }
        };

    private List<DirectMessageHandler> mHandlers = new ArrayList<DirectMessageHandler>();

    abstract class DirectMessageHandler{
        abstract boolean willHandle(String fromId, JSONObject msg);
        abstract void handle(String fromId, JSONObject msg);
    }

    class InviteToSharedAppHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("invite_app_session");
        }
        void handle(String fromId, JSONObject obj){
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
                DungBeetleService.this, "Invitation received", 
                "Click to launch application.", 
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
        }
    }

    class InviteToSharedAppFeedHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("invite_app_feed");
        }
        void handle(String fromId, JSONObject obj){
            long contactId = mIdent.contactIdForPersonId(fromId);
            /*
              handle multiparty invitation,
              add fromContactId to intent as "sender"
              get particpant personids list out of obj
            */
            // String packageName = obj.optString(InviteObj.PACKAGE_NAME);
            // String arg = obj.optString(InviteObj.ARG);
            // Intent launch = new Intent();
            // launch.setAction(Intent.ACTION_MAIN);
            // launch.addCategory(Intent.CATEGORY_LAUNCHER);
            // launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
            // launch.putExtra("creator", false);
            // launch.setPackage(packageName);
            // final PackageManager mgr = getPackageManager();
            // List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
            // if (resolved.size() == 0) {
            //     Toast.makeText(DungBeetleService.this, 
            //                    "Could not find application to handle invite", 
            //                    Toast.LENGTH_SHORT).show();
            //     return;
            // }
            // ActivityInfo info = resolved.get(0).activityInfo;
            // launch.setComponent(new ComponentName(
            //                         info.packageName,
            //                         info.name));
            // Notification notification = new Notification(
            //     R.drawable.icon, "New Invitation", System.currentTimeMillis());
            // PendingIntent contentIntent = PendingIntent.getActivity(
            //     DungBeetleService.this, 0, launch, 0);
            // notification.setLatestEventInfo(
            //     DungBeetleService.this, "Invitation received", 
            //     "Click to launch application.", 
            //     contentIntent);
            // notification.flags = Notification.FLAG_AUTO_CANCEL;
            // mNotificationManager.notify(0, notification);
        }
    }


    class InviteToWebSessionHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("invite_web_session");
        }
        void handle(String fromId, JSONObject obj){
            /*
              handle multiparty invitation,
              add fromContactId to intent as "sender"
              get particpant personids list out of obj
            */
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


    class SendFileHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("send_file");
        }
        void handle(String fromId, JSONObject obj){
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


    class IMHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("instant_message");
        }
        void handle(String fromId, JSONObject obj){
            String msg = obj.optString("text");
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(
                                    getPackageName(),
                                    DungBeetleActivity.class.getName()));
            Notification notification = new Notification(
                R.drawable.icon, "New IM", System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(
                DungBeetleService.this, 0, launch, 0);
            notification.setLatestEventInfo(
                DungBeetleService.this, "New IM", "\"" + msg + "\"", contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
        }
    }


    class SubscribeReqHandler extends DirectMessageHandler{
        boolean willHandle(String fromId, JSONObject msg){ 
            return msg.optString("type").equals("subscribe_req");
        }
        void handle(String fromId, JSONObject obj){
            String feedName = obj.optString("feedName");
            long contactId = mIdent.contactIdForPersonId(fromId);
            Helpers.insertSubscriber(DungBeetleService.this, contactId, feedName);
        }
    }


    private void handleIncomingDirectMessage(IncomingMessage incoming){
        String contents = incoming.contents();
        String personId = incoming.from();
        try{
            JSONObject obj = new JSONObject(contents);
            for(DirectMessageHandler h : mHandlers){
                if(h.willHandle(personId, obj)){
                    h.handle(personId, obj);
                    break;
                }
            }
        }
        catch(JSONException e){ throw new RuntimeException(e); }
    }

    @Override
    public void onCreate() {
        mIdent = new DBIdentityProvider(new DBHelper(this));
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mManagerThread = new ManagerThread(this, mToastHandler, mDirectMessageHandler);
        mManagerThread.start();
        getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/in"), true, 
            new ContentObserver(new Handler(getMainLooper())) {
                @Override
                public synchronized void onChange(boolean self) {
                    
                }});
        mHandlers.add(new SubscribeReqHandler());
        mHandlers.add(new IMHandler());
        mHandlers.add(new InviteToWebSessionHandler());
        mHandlers.add(new InviteToSharedAppFeedHandler());
        mHandlers.add(new SendFileHandler());
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
        mIdent.close();
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


}