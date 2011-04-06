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
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import java.util.List;
import org.json.JSONObject;


public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private ManagerThread mManagerThread;

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
                JSONObject obj = (JSONObject)msg.obj;
                handleIncomingDirectMessage(obj);
            }
        };

    private void handleIncomingDirectMessage(JSONObject obj){
        String type = obj.optString("type");
        if(type.equals(InviteObj.TYPE)){
            handleAppInvitation(obj);
        }
        else if(type.equals("send_file")){
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
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
            notification.setLatestEventInfo(
                this, "New Shared File",
                mimeType + "  " + uri,
                contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
        }
        else if(type.equals("instant_message")){
            String msg = obj.optString("text");
            Intent launch = new Intent();
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(
                                    getPackageName(),
                                    DungBeetleActivity.class.getName()));
            Notification notification = new Notification(
                R.drawable.icon, "New IM", System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launch, 0);
            notification.setLatestEventInfo(this, "New IM", "\"" + msg + "\"", contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(0, notification);
        }
    }

    private void handleAppInvitation(JSONObject obj){
        String packageName = obj.optString(InviteObj.PACKAGE_NAME);
        String arg = obj.optString(InviteObj.ARG);
        Intent launch = new Intent();
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
        launch.putExtra("creator", false);
        if(obj.has(InviteObj.WEB_URL)){
        	String webUrl = obj.optString(InviteObj.WEB_URL);
            launch.setData(Uri.parse(webUrl));
        }
        else{
            launch.setPackage(packageName);
            final PackageManager mgr = getPackageManager();
            List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
            if (resolved.size() == 0) {
                Toast.makeText(this, "Could not find application to handle invite", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Wouldn't it be cool if the app auto installed? Yeah. It will be cool.", Toast.LENGTH_SHORT).show();
                return;
            }
            ActivityInfo info = resolved.get(0).activityInfo;
            launch.setComponent(new ComponentName(
                                    info.packageName,
                                    info.name));
        }

        Notification notification = new Notification(
            R.drawable.icon, "New Invitation", System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launch, 0);
        notification.setLatestEventInfo(
            this, "Invitation received", 
            "Click to launch application.", 
            contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(0, notification);
    }

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        mManagerThread = new ManagerThread(this, mToastHandler, mDirectMessageHandler);
        mManagerThread.start();
        getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/in"), true, 
            new ContentObserver(new Handler(getMainLooper())) {
                @Override
                public synchronized void onChange(boolean self) {
                    
                }});
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
