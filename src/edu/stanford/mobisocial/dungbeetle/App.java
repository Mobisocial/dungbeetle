package edu.stanford.mobisocial.dungbeetle;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import edu.stanford.mobisocial.dungbeetle.util.ImageCache;


public class App extends Application {

    public final ImageCache contactImages = new ImageCache(30);
    public final ImageCache objectImages = new ImageCache(30);
    private ScreenState mScreenState;

    private static App instance;

    public static App instance(){
        if(instance != null) return instance;
        else throw new IllegalStateException("WTF, why no App instance.");
    }

    public boolean isScreenOn(){
        return !mScreenState.isOff;
    }

	@Override
	public void onCreate() {
		super.onCreate();
        App.instance = this;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenState = new ScreenState();
        getApplicationContext().registerReceiver(mScreenState, filter);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

    private class ScreenState extends BroadcastReceiver {
        public boolean isOff = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isOff = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                isOff = false;
            }
        }

    }


}
