package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;


public class AppReferenceObj implements DbEntryHandler, FeedRenderer, Activator, FeedMessageHandler {
	private static final String TAG = "InviteToSharedAppObj";
	private static final boolean DBG = false;

    public static final String TYPE = "invite_app_session";
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

    public static AppReference from(String packageName, String arg, String feedName, String groupUri) {
        return new AppReference(json(packageName, arg, feedName, groupUri));
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
    public void handleReceived(Context context, Contact from, JSONObject obj) {
        String packageName = obj.optString(PACKAGE_NAME);
        String arg = obj.optString(ARG);
        Intent launch = new Intent();
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra(AppReference.EXTRA_APPLICATION_ARGUMENT, arg);
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

	public void render(final Context context, final ViewGroup frame, JSONObject content) {
	    // TODO: hack to show object history in app feeds
        JSONObject appState = getAppState(context, content);
        if (appState != null) {
            content = appState;
        } else {
            Log.wtf(TAG, "Missing inner content");
        }

	    boolean rendered = false;
	    AppReference ref = new AppReference(content);
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
            webview.setOnTouchListener(new WebViewClickListener(webview, frame, (Integer)o));
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
	public void activate(Uri feed, Context context, final JSONObject content) {
	    if (DBG) Log.d(TAG, "activating " + content);

	    if (!content.has(DbObject.CHILD_FEED_NAME)) {
            Log.wtf(TAG, "Bad app reference found.");
            Toast.makeText(context, "Could not launch application.", Toast.LENGTH_SHORT).show();
            return;
        }

	    String arg = content.optString(ARG);
	    String state = content.optString(STATE);

	    String appId;
	    Uri appFeed = feed;

	    appId = content.optString(PACKAGE_NAME);
	    appFeed = Feed.uriForName(content.optString(DbObject.CHILD_FEED_NAME));
	    JSONObject appContent = getAppState(context, content);
	    if (appContent != null) {
	        if (DBG) Log.d(TAG, "transformed to " + appContent);
            arg = appContent.optString(ARG);
            state = appContent.optString(STATE);
	    }

	    if (DBG) Log.d(TAG, "Preparing launch of " + appId);
	    Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
	    launch.putExtra(AppReference.EXTRA_FEED_URI, appFeed);
	    if (arg != null) {
	        launch.putExtra(AppReference.EXTRA_APPLICATION_ARGUMENT, arg);
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
	            context.startActivity(launch);
	            return;
	        }
	    }

        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId));
        context.startActivity(market);
	    //Toast.makeText(context, "No activity found.", Toast.LENGTH_SHORT).show();
	}

	public static void promptForApplication(final Context context, final Callback callback) {
	    final PackageManager mgr = context.getPackageManager();
	    final List<ResolveInfo> availableAppInfos = new ArrayList<ResolveInfo>();
	    Intent i = new Intent();

	    /** 2-player applications **/
	    List<ResolveInfo> infos;
	    i.setAction("mobisocial.intent.action.TWO_PLAYER");
        i.addCategory("android.intent.category.LAUNCHER");
        i.addCategory("android.intent.category.DEFAULT");
        infos = mgr.queryIntentActivities(i, 0);
        availableAppInfos.addAll(infos);
        final int numTwoPlayer = infos.size();

	    /** Negotiate p2p connectivity out-of-band **/
        i = new Intent();
        i.setAction("android.intent.action.CONFIGURE");
        i.addCategory("android.intent.category.P2P");
        infos = mgr.queryBroadcastReceivers(i, 0);
        availableAppInfos.addAll(infos);
        if (availableAppInfos.isEmpty()) {
            Toast.makeText(context.getApplicationContext(),
                    "Sorry, couldn't find any compatible apps.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> names = new ArrayList<String>();
        for(ResolveInfo info : availableAppInfos){
            names.add(info.loadLabel(mgr).toString());
        }
        final CharSequence[] items = names.toArray(new CharSequence[]{});
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share application:");
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    final ResolveInfo info = availableAppInfos.get(item);
                    Intent i = new Intent();
                    i.setClassName(info.activityInfo.packageName, 
                                   info.activityInfo.name);
                    if (item < numTwoPlayer) {
                        i.setAction("mobisocial.intent.action.TWO_PLAYER");
                        i.addCategory("android.intent.category.LAUNCHER");
                        callback.onAppSelected(info.activityInfo.packageName, null, i);
                        return;
                    } else {
                        i.setAction("android.intent.action.CONFIGURE");
                        i.addCategory("android.intent.category.P2P");
                    }

                    BroadcastReceiver rec = new BroadcastReceiver() {
                            public void onReceive(Context c, Intent i){
                                Intent launch = new Intent();
                                launch.setAction(Intent.ACTION_MAIN);
                                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                                launch.setPackage(info.activityInfo.packageName);
                                List<ResolveInfo> resolved = 
                                    mgr.queryIntentActivities(launch, 0);
                                if (resolved.size() > 0) {
                                    ActivityInfo info = resolved.get(0).activityInfo;
                                    String arg = getResultData();
                                    launch.setComponent(new ComponentName(info.packageName,
                                            info.name));
                                    launch.putExtra("creator", true);
                                    launch.putExtra(
                                        "android.intent.extra.APPLICATION_ARGUMENT", arg);
                                    callback.onAppSelected(info.packageName, arg, launch);
                                }
                                else{
                                    Toast.makeText(context, "No applications found.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        };

                    context.sendOrderedBroadcast(i, null, rec, null, Activity.RESULT_OK, null, null);
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
	}

   private JSONObject getAppState(Context context, JSONObject appReference) {
        if (appReference.has(DbObject.CHILD_FEED_NAME)) {
            String feedName = appReference.optString(DbObject.CHILD_FEED_NAME);
            Uri feedUri = Feed.uriForName(feedName);
            String selection = "type in ('" + TYPE + "')";
            String[] projection = new String[] {"json"};
            String order = "_id desc LIMIT 1";
            Cursor c = context.getContentResolver().query(feedUri, projection, selection, null, order);
            if (c.moveToFirst()) {
                try {
                    return new JSONObject(c.getString(0));
                } catch (JSONException e) {
                    Log.wtf(TAG, "not really json", e);
                } finally {
                    c.close();
                }
            } else {
                c.close();
            }
        }
        return null;
    }

	public interface Callback {
	    public void onAppSelected(String pkg, String arg, Intent localLaunch);
	}

    /**
     * Subscribe to the application feed automatically.
     * TODO, work out observers vs. players.
     */
    @Override
    public void handleFeedMessage(Context context, Uri feedUri, JSONObject obj) {
        if (obj.has(DbObject.CHILD_FEED_NAME)) {
            String feedName = obj.optString(DbObject.CHILD_FEED_NAME);
            DBHelper helper = new DBHelper(context);
            Maybe<Group> mg = helper.groupByFeedName(feedName);
            helper.close();
            if (!mg.isKnown() && obj.has(GROUP_URI)) {
                Uri gUri = Uri.parse(obj.optString(GROUP_URI));
                Group.join(context, gUri);
            }
        }
    }

    private class WebViewClickListener implements View.OnTouchListener {
        private int position;
        private ViewGroup vg;
        private ViewGroup frame;
        private ListView lv;

        public WebViewClickListener(WebView wv, ViewGroup vg, int position) {
            this.vg = vg;
            this.position = position;
        }

        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action) {
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
                    vg = (ViewGroup)vg.getParent();
                }
                lv = (ListView) vg;
            }
            lv.performItemClick(frame, position, 0);
        }
    }
}
