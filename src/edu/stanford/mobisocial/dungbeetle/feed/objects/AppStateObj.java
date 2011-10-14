package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

/**
 * A snapshot of an application's state.
 */
@Deprecated
public class AppStateObj extends DbEntryHandler implements FeedRenderer, Activator {
	private static final String TAG = "AppStateObj";
	private static final boolean DBG = true;

    public static final String TYPE = "appstate";
    public static final String ARG = "arg";
    public static final String STATE = "state";
    public static final String THUMB_JPG = "b64jpgthumb";
    public static final String THUMB_TEXT = "txt";
    public static final String THUMB_HTML = "html";
    public static final String PACKAGE_NAME = "packageName";
    public static final String GROUP_URI = "groupuri";

    @Override
    public String getType() {
        return TYPE;
    }

    public static AppState from(String packageName, String arg, String feedName, String groupUri) {
        return new AppState(json(packageName, arg, feedName, groupUri));
    }

    public static JSONObject json(String packageName, String arg, String feedName, String groupUri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PACKAGE_NAME, packageName);
            obj.put(ARG, arg);
            obj.put(DbObject.CHILD_FEED_NAME, feedName);
            obj.put(GROUP_URI, groupUri);
        } catch(JSONException e){}
        return obj;
    }

    public static JSONObject json(String packageName, String arg, String state,
            String b64JpgThumb, String thumbText, String feedName, String groupUri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PACKAGE_NAME, packageName);
            obj.put(ARG, arg);
            if (state != null) {
                obj.put(STATE, state);
            }
            if (b64JpgThumb != null) {
                obj.put(THUMB_JPG, b64JpgThumb);
            }
            if (thumbText != null) {
                obj.put(THUMB_TEXT, thumbText);
            }
            if (feedName != null) {
                obj.put(DbObject.CHILD_FEED_NAME, feedName);
            }
            if (groupUri != null) {
                obj.put(GROUP_URI, groupUri);
            }
        } catch(JSONException e) {}
        return obj;
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject obj) {

    }

	public void render(final Context context, final ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
	    // TODO: hack to show object history in app feeds
        JSONObject appState = getAppState(context, content);
        if (appState != null) {
            content = appState;
        } else {
            Log.e(TAG, "Missing inner content, probably because of format changes");
        }

	    boolean rendered = false;
	    AppState ref = new AppState(content);
	    String thumbnail = ref.getThumbnailImage();
	    if (thumbnail != null) {
	        rendered = true;
	        ImageView imageView = new ImageView(context);
	        imageView.setLayoutParams(new LinearLayout.LayoutParams(
	                                      LinearLayout.LayoutParams.WRAP_CONTENT,
	                                      LinearLayout.LayoutParams.WRAP_CONTENT));
	        App.instance().objectImages.lazyLoadImage(thumbnail.hashCode(), thumbnail, imageView);
	        frame.addView(imageView);
	    }

	    thumbnail = ref.getThumbnailText();
	    if (thumbnail != null) {
	        rendered = true;
            TextView valueTV = new TextView(context);
            valueTV.setText(thumbnail);
            valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
            frame.addView(valueTV);
	    }

	    thumbnail = ref.getThumbnailHtml();
        if (thumbnail != null) {
            rendered = true;
            WebView webview = new WebView(context);
            webview.loadData(thumbnail, "text/html", "UTF-8");
            webview.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
            Object o = frame.getTag(R.id.object_entry);
            webview.setOnTouchListener(new WebViewClickListener(frame, (Integer)o));
            frame.addView(webview);
        }

	    if (!rendered) {
	        String appName = content.optString(PACKAGE_NAME);
	        if (appName.contains(".")) {
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
	    Intent launch = getLaunchIntent(context, content);

	    // TODO: Temporary, while transitioning to AppObj
        launch.putExtra("obj", content.toString());
	    if (!(context instanceof Activity)) {
	        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    }
	    context.startActivity(launch);
	}

	public static Intent getLaunchIntent(Context context, JSONObject content) {
	    Log.d(TAG, "Getting launch intent for " + content);
	    Uri  appFeed;
	    if (content.has(DbObject.CHILD_FEED_NAME)) {
	        appFeed = Feed.uriForName(content.optString(DbObject.CHILD_FEED_NAME));
	    } else {
	        appFeed = Feed.uriForName(content.optString(DbObject.FEED_NAME));
	    }
	    String arg = content.optString(ARG);
        String state = content.optString(STATE);
	    String appId = content.optString(PACKAGE_NAME); // Not DbObject.APP_ID!
	    if (DBG) Log.d(TAG, "Preparing launch of " + appId);
	    
	    Intent launch = new Intent();
	    if (content.has(AppReferenceObj.OBJ_INTENT_ACTION)) {
	        launch.setAction(content.optString(AppReferenceObj.OBJ_INTENT_ACTION));
	    } else {
	        launch.setAction(Intent.ACTION_MAIN);
	    }
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra(AppState.EXTRA_FEED_URI, appFeed);
        // TODO: hack until this obj is available in 'related' query.
        launch.putExtra("obj", content.toString());
        if (arg != null) {
            launch.putExtra(AppState.EXTRA_APPLICATION_ARGUMENT, arg);
        }
        // TODO: optimize!
        List<ResolveInfo> resolved = context.getPackageManager().queryIntentActivities(launch, 0);
        for (ResolveInfo r : resolved) {
            ActivityInfo activity = r.activityInfo;
            if (activity.packageName.equals(appId)) {
                launch.setClassName(activity.packageName, activity.name);
                launch.putExtra("mobisocial.db.PACKAGE", activity.packageName);
                if (state != null) {
                    launch.putExtra("mobisocial.db.STATE", state);
                }
                return launch;
            }
        }

        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
        return market;
	}

   private JSONObject getAppState(Context context, JSONObject appReference) {
        if (appReference.has(DbObject.CHILD_FEED_NAME)) {
            String feedName = appReference.optString(DbObject.CHILD_FEED_NAME);
            Uri feedUri = Feed.uriForName(feedName);
            String selection = "type in ('" + TYPE + "')";
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
        }
        return null;
    }

	public interface Callback {
	    public void onAppSelected(String pkg, String arg, Intent localLaunch);
	}

    private class WebViewClickListener implements View.OnTouchListener {
        private int position;
        private ViewGroup vg;
        private ViewGroup frame;
        private ListView lv;

        public WebViewClickListener(ViewGroup vg, int position) {
            this.vg = vg;
            this.position = position;
        }

        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_CANCEL:
                    return true;
                case MotionEvent.ACTION_UP:
                    sendClick();
                    return true;
            }
            return false;
        }

        public void sendClick() {
            if (lv == null) {
                while (!(vg instanceof ListView)) {
                    if (null != vg.getTag(R.id.object_entry)) {
                        frame = vg;
                    }
                    vg = (ViewGroup) vg.getParent();
                    if (vg.performClick()) {
                        return;
                    }
                }
                lv = (ListView) vg;
            }
            lv.performItemClick(frame, position, 0);
        }
    }
}
