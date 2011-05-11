package edu.stanford.mobisocial.dungbeetle;
import android.app.Application;
import edu.stanford.mobisocial.dungbeetle.util.ContactImageCache;


public class App extends Application {

    public final ContactImageCache contactImages = new ContactImageCache(30);

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

}
