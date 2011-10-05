package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.List;

import mobisocial.socialkit.musubi.multiplayer.Multiplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

/**
 * A pointer to an application instance feed.
 * {@see LaunchApplicationAction}
 */
@Deprecated
public class AppReferenceObj extends DbEntryHandler
        implements FeedRenderer, Activator, FeedMessageHandler {
	private static final String TAG = "AppReferenceObj";
	private static final boolean DBG = true;

    public static final String TYPE = "invite_app_session";
    public static final String ARG = "arg";
    public static final String PACKAGE_NAME = "packageName";
    public static final String GROUP_URI = "groupuri";
    public static final String CREATOR_ID = "creator_id";

    private final AppStateObj mAppStateObj = new AppStateObj();

    @Override
    public String getType() {
        return TYPE;
    }

	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public static DbObject from(String packageName, String arg,
	        String feedName, String groupUri, long creatorId) {
        return new DbObject(TYPE, json(packageName, arg, feedName, groupUri, creatorId));
    }

	// TODO: Bundle <=> Json
	public static DbObject forFixedMembership(String packageName, String[] membership,
	        String feedName, String groupUri) {

	    JSONObject json = new JSONObject();
	    try {
	        JSONArray mship = new JSONArray();
	        for (String m : membership) {
	            mship.put(m);
	        }

	        json.put(PACKAGE_NAME, packageName);
	        json.put(Multiplayer.OBJ_MEMBERSHIP, mship);
	        json.put(DbObject.CHILD_FEED_NAME, feedName);
	        json.put(GROUP_URI, groupUri);
        } catch(JSONException e){}
        return new DbObject(TYPE, json);
    }

    public static JSONObject json(String packageName, String arg,
            String appFeedName, String appGroupUri, long creatorId) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PACKAGE_NAME, packageName);
            obj.put(ARG, arg);
            obj.put(DbObject.CHILD_FEED_NAME, appFeedName);
            obj.put(GROUP_URI, appGroupUri);
            obj.put(CREATOR_ID, creatorId);
        } catch(JSONException e){}
        return obj;
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
        String packageName = obj.optString(PACKAGE_NAME);
        String arg = obj.optString(ARG);
        Intent launch = new Intent();
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra(AppState.EXTRA_APPLICATION_ARGUMENT, arg);
        launch.putExtra("creator", false);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.setPackage(packageName);
        final PackageManager mgr = context.getPackageManager();
        List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
        if (resolved == null || resolved.size() == 0) {
            Toast.makeText(context, 
                           "Could not find application to handle invite", 
                           Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityInfo info = resolved.get(0).activityInfo;
        launch.setComponent(new ComponentName(
                                info.packageName,
                                info.name));
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

        (new PresenceAwareNotify(context)).notify(
            "New Invitation",
            "Invitation received from " + from.name, 
            "Click to launch application.", 
            contentIntent);
    }

	public void render(final Context context, final ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
	    // TODO: hack to show object history in app feeds
        JSONObject appState = getAppState(context, content);
        if (appState != null) {
            mAppStateObj.render(context, frame, appState, raw, allowInteractions);
            return;
        } else {
	        String appName = content.optString(PACKAGE_NAME);
	        if (appName.contains(".")) {
	            // TODO: look up label.
	            appName = appName.substring(appName.lastIndexOf(".") + 1);
	        }
            String text = "Welcome to " + appName + "!";
            TextView valueTV = new TextView(context);
            valueTV.setText(text);
            valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
            frame.addView(valueTV);
        }
    }

	@Override
	public void activate(Context context, long contactId, JSONObject content, byte[] raw) {
	    if (DBG) Log.d(TAG, "activating " + content);

	    if (!content.has(DbObject.CHILD_FEED_NAME)) {
            Log.wtf(TAG, "Bad app reference found.");
            Toast.makeText(context, "Could not launch application.", Toast.LENGTH_SHORT).show();
            return;
        }

	    JSONObject appContent = getAppState(context, content);
	    if (appContent == null) {
	        Uri appFeed = Feed.uriForName(content.optString(DbObject.CHILD_FEED_NAME));
	        String appId = content.optString(PACKAGE_NAME);
	        String arg = content.optString(ARG);
	        String state = null;

	        Intent launch = AppStateObj.getLaunchIntent(context, appId, arg, state, appFeed);
	        // TODO: temporary.
	        launch.putExtra("obj", content.toString());
	        if (!(context instanceof Activity)) {
	            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        }

	        context.startActivity(launch);
	    } else {
            if (DBG) Log.d(TAG, "pulled app state " + appContent);
            mAppStateObj.activate(context, Contact.MY_ID, appContent, raw);
	    }
	}

   private JSONObject getAppState(Context context, JSONObject appReference) {
        Log.w(TAG, appReference.toString());
        if (appReference.has(DbObject.CHILD_FEED_NAME)) {
            String feedName = appReference.optString(DbObject.CHILD_FEED_NAME);
            Uri feedUri = Feed.uriForName(feedName);
            String selection = "type in ('" + AppStateObj.TYPE + "')";
            String[] projection = new String[] {"json"};
            String order = "_id desc LIMIT 1";
            Cursor c = context.getContentResolver().query(feedUri, projection, selection, null, order);
            try {
	            if (c.moveToFirst()) {
	            	try {
	                    return new JSONObject(c.getString(0));
	                } catch (JSONException e) {
	                    Log.e(TAG, "not really json", e);
	                }
	            }
        	} finally {
        		c.close();
        	}
        } else if (appReference.has("state")) {
            return appReference;
        }
        return null;
    }

    /**
     * Subscribe to the application feed automatically.
     * TODO, work out observers vs. players.
     */
    @Override
    public void handleFeedMessage(Context context, Uri feedUri, long contactId, long sequenceId,
            String type, JSONObject obj) {
        if (obj.has(DbObject.CHILD_FEED_NAME)) {
            String feedName = obj.optString(DbObject.CHILD_FEED_NAME);
            DBHelper helper = DBHelper.getGlobal(context);
            Maybe<Group> mg = helper.groupByFeedName(feedName);
            helper.close();
            if (!mg.isKnown() && obj.has(GROUP_URI)) {
                Uri gUri = Uri.parse(obj.optString(GROUP_URI));
                Group.join(context, gUri);
            }
        }
    }
}
