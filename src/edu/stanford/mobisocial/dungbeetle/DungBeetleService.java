package edu.stanford.mobisocial.dungbeetle;
import android.os.Message;
import android.os.Handler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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


    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        mManagerThread = new ManagerThread(this, mToastHandler);
        mManagerThread.start();
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
        return binder_;
    }


	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class DungBeetleBinder extends Binder {
    	DungBeetleService getService() {
            return DungBeetleService.this;
        }
    }


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder binder_ = new DungBeetleBinder();


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
            new Intent(this, Setup.class), 0);

        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                                        text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNotificationManager.notify(R.string.active, notification);
    }
}