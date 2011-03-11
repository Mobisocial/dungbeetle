package org.stanford.mobisocial.dungbeetle;

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
	NotificationManager notificationManager_;
	MailInboxThread inbox_;
	MailMrPrivacyThread mrprivacy_;
	MailSendThread sender_;

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
    @Override
    public void onCreate() {
        notificationManager_ = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MrpService", "Received start id " + startId + ": " + intent);
        inbox_ = new MailInboxThread();
        new Thread(inbox_).start();
        mrprivacy_ = new MailMrPrivacyThread(this);
        new Thread(mrprivacy_).start();
        sender_ = new MailSendThread(this);
        new Thread(sender_).start();
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager_.cancel(R.string.mrp_active);
        // Tell the user we stopped.
        Toast.makeText(this, R.string.mrp_stop, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder_;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder binder_ = new MrpBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.mrp_start);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Setup.class), 0);
        
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        notificationManager_.notify(R.string.mrp_active, notification);
    }
}