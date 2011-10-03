package edu.stanford.mobisocial.dungbeetle;
import java.math.BigInteger;
import java.security.SecureRandom;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ImageCache;


public class App extends Application {
    public static final String TAG = "musubi";
    public final ImageCache contactImages = new ImageCache(30);
    public final ImageCache objectImages = new ImageCache(30);
    private ScreenState mScreenState;

    private static App instance;
    private SecureRandom secureRandom;
    private Uri mFeedUri;

    private String mLocalPersonId;

    public static App instance(){
        if(instance != null) return instance;
        else throw new IllegalStateException("WTF, why no App instance.");
    }

    public boolean isScreenOn(){
        return !mScreenState.isOff;
    }

    public String getRandomString() {
        return new BigInteger(130, secureRandom).toString(32);
    }

	@Override
	public void onCreate() {
		super.onCreate();
        App.instance = this;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenState = new ScreenState();
        getApplicationContext().registerReceiver(mScreenState, filter);
        secureRandom = new SecureRandom();
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

    public String getLocalPersonId() {
        if (mLocalPersonId == null) {
            initLocalUser();
        }
        return mLocalPersonId;
    }

    private void initLocalUser() {
        DBHelper mHelper = DBHelper.getGlobal(this);
        DBIdentityProvider mIdent = new DBIdentityProvider(mHelper);

        mLocalPersonId = mIdent.userPersonId();

        mIdent.close();
        mHelper.close();
    }

    public void setCurrentFeed(Uri feedUri) {
        mFeedUri = feedUri;
        if (feedUri != null) {
            resetUnreadMessages(feedUri);
        }
    }

    public Uri getCurrentFeed() {
        return mFeedUri;
    }

    private void resetUnreadMessages(Uri feedUri) {
        try {
            switch(Feed.typeOf(feedUri)) {
	            case Feed.FEED_GROUP: {
	                String feedName = feedUri.getLastPathSegment();
	                ContentValues cv = new ContentValues();
	                cv.put(Group.NUM_UNREAD, 0);
	
	                getContentResolver().update(
	                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Group.TABLE), cv,
	                        Group.FEED_NAME + "='" + feedName + "'", null);
	                getContentResolver().notifyChange(
	                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist"), null);
	
	            	break;
	            }
	            case Feed.FEED_FRIEND: {
	            	long contact_id = Long.valueOf(feedUri.getLastPathSegment());
	                ContentValues cv = new ContentValues();
	                cv.put(Contact.NUM_UNREAD, 0);
	                getContentResolver().update(
	                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Contact.TABLE), cv,
	                        Contact._ID + "='" + contact_id + "'", null);
	            	break;
	            } 
	            case Feed.FEED_RELATED: {
	            	//TODO: hmm?
	            	break;
	            }
        	}
        } catch (Exception e) {
            Log.e(TAG, "Error clearing unread messages", e);
        }
    }
}
