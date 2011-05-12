package edu.stanford.mobisocial.dungbeetle;
import android.app.Application;
import edu.stanford.mobisocial.dungbeetle.util.ImageCache;


public class App extends Application {

    public final ImageCache contactImages = new ImageCache(30);
    public final ImageCache objectImages = new ImageCache(30);

    private static App instance;

    public static App instance(){
        if(instance != null) return instance;
        else throw new IllegalStateException("WTF, why no App instance.");
    }

	@Override
	public void onCreate() {
        App.instance = this;
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

}
