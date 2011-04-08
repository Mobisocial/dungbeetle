package edu.stanford.mobisocial.dungbeetle;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private MessagingManagerThread mManagerThread;
    private DBHelper mHelper;
    public static final String TAG = "DungBeetleService";


    @Override
    public void onCreate() {
        mHelper = new DBHelper(this);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mManagerThread = new MessagingManagerThread(this);
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


}
