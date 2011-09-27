package edu.stanford.mobisocial.dungbeetle;
import org.mobisocial.corral.ContentCorral;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private MessagingManagerThread mMessagingManagerThread;
	private GroupManagerThread mGroupManagerThread;
	private ContentCorral mContentCorral;
    private DBHelper mHelper;
    public static final String TAG = "DungBeetleService";


    @Override
    public void onCreate() {
        mHelper = DBHelper.getGlobal(this);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mMessagingManagerThread = new MessagingManagerThread(this);
        mMessagingManagerThread.start();

        mGroupManagerThread = new GroupManagerThread(this);
        mGroupManagerThread.start();

        // mPresenceThread = new PresenceThread(this);
        // mPresenceThread.start();

        // TODO: content corral should manage it's own ip ups and downs.
        mContentCorral = new ContentCorral(this);
        mContentCorral.start();
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
        mMessagingManagerThread.interrupt();
        mGroupManagerThread.interrupt();
//        mPresenceThread.interrupt();
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
