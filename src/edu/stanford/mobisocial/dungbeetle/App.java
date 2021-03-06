/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;

import java.math.BigInteger;
import java.security.SecureRandom;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ImageCache;

public class App extends Application {
    /**
     * The protocol version we speak, affecting things like wire protocol
     * format and physical network support, available features, app api, etc.
     */
    public static final String PREF_POSI_VERSION = "posi_version";
    public static final int POSI_VERSION = 4;

    public static final String TAG = "musubi";
    public final ImageCache contactImages = new ImageCache(30);
    public final ImageCache objectImages = new ImageCache(30);
    private ScreenState mScreenState;

    private static App instance;
    private SecureRandom secureRandom;
    private Uri mFeedUri;
    private Musubi mMusubi;

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
        mMusubi = Musubi.getInstance(getApplicationContext());

        // Sync profile information.
        SharedPreferences prefs = getSharedPreferences("main", 0);
        int oldVersion = prefs.getInt(PREF_POSI_VERSION, 0);
        if (oldVersion <= POSI_VERSION) {
            Obj updateObj = ProfileObj.getLocalProperties(this);
            Log.d(TAG, "Broadcasting new profile attributes: " + updateObj.getJson());
            Helpers.sendToEveryone(this, updateObj);
            prefs.edit().putInt(PREF_POSI_VERSION, POSI_VERSION).commit();
        }
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

    public Musubi getMusubi() {
        return mMusubi;
    }

    private void resetUnreadMessages(Uri feedUri) {
        try {
            switch(Feed.typeOf(feedUri)) {
                case FRIEND: {
                    String personId = Feed.friendIdForFeed(feedUri);
                    ContentValues cv = new ContentValues();
                    cv.put(Contact.NUM_UNREAD, 0);
                    getContentResolver().update(
                            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Contact.TABLE), cv,
                            Contact.PERSON_ID + "='" + personId + "' AND " +
                            Contact.NUM_UNREAD + " != 0", null);
                    // No break; do group feed too.
                    // TODO, get rid of person msg count?
                }
	            case GROUP: {
	                String feedName = feedUri.getLastPathSegment();
	                ContentValues cv = new ContentValues();
	                cv.put(Group.NUM_UNREAD, 0);
	
	                int r = getContentResolver().update(
	                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Group.TABLE), cv,
	                        Group.FEED_NAME + "='" + feedName + "' AND " +
	                        Group.NUM_UNREAD + " != 0", null);
                    if (r > 0) {
                        getContentResolver().notifyChange(Uri.parse(
                                DungBeetleContentProvider.CONTENT_URI + "/feeds"), null);
	                }
	            	break;
	            }
	            case RELATED: {
	            	//TODO: hmm?
	            	break;
	            }
        	}
        } catch (Exception e) {
            Log.e(TAG, "Error clearing unread messages", e);
        }
    }
}
