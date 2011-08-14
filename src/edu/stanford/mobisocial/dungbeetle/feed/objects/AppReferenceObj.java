package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.view.Gravity;
import android.view.ViewGroup;

import java.util.ArrayList;
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
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

import java.util.Iterator;


public class AppReferenceObj implements DbEntryHandler, FeedRenderer, Activator {
	private static final String TAG = "InviteToSharedAppObj";

    public static final String TYPE = "invite_app_session";
    public static final String ARG = "arg";
    public static final String STATE = "state";
    public static final String THUMB_JPG = "b64jpgthumb";
    public static final String THUMB_TEXT = "txt";
    public static final String PACKAGE_NAME = "packageName";
    public static final String APP_IDENTIFIER = "sid";

    @Override
    public String getType() {
        return TYPE;
    }

    public static AppReference from(String packageName, String arg, String feedName) {
        return new AppReference(json(packageName, arg, feedName));
    }

    public static JSONObject json(String packageName, String arg, String feedName) {
        JSONObject obj = new JSONObject();
        try{
            obj.put(PACKAGE_NAME, packageName);
            obj.put(ARG, arg);
            obj.put(APP_IDENTIFIER, feedName);
        }catch(JSONException e){}
        return obj;
    }

    public static JSONObject json(String packageName,
            String arg, String state, String b64JpgThumb, String thumbText, String feedName) {
        JSONObject obj = new JSONObject();
        try{
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
                obj.put(APP_IDENTIFIER, feedName);
            }
        } catch(JSONException e) {}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        String packageName = obj.optString(PACKAGE_NAME);
        String arg = obj.optString(ARG);
        Log.i(TAG, "Received invite with arg: " + arg);
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

	public void render(final Context context, final ViewGroup frame, final JSONObject content) {
	    AppReference ref = new AppReference(content);
	    String thumbnail = ref.getThumbnailImage();
	    if (thumbnail != null) {
	        ImageView imageView = new ImageView(context);
	        imageView.setLayoutParams(new LinearLayout.LayoutParams(
	                                      LinearLayout.LayoutParams.WRAP_CONTENT,
	                                      LinearLayout.LayoutParams.WRAP_CONTENT));
	        App.instance().objectImages.lazyLoadImage(thumbnail.hashCode(), thumbnail, imageView);
	        frame.addView(imageView);
	    } else {
	        String text = ref.getThumbnailText();
	        if (ref.getThumbnailText() == null) {
	            text = content.optString(ARG);
	        }
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
	public void activate(Uri feed, Context context, JSONObject content) {
	    AppReference app = new AppReference(content);
	    Intent launch = new Intent(Intent.ACTION_MAIN);
	    launch.addCategory(Intent.CATEGORY_LAUNCHER);
	    Uri appFeed;
	    if (content.has(APP_IDENTIFIER)) {
	        appFeed = Feed.uriForName(content.optString(APP_IDENTIFIER));
	    } else {
	        Log.w(TAG, "Warning: no dedicated app feed; using parent feed.");
	        appFeed = feed;
	    }
	    launch.putExtra(AppReference.EXTRA_FEED_URI, appFeed);

	    if (content.has(ARG)) {
	        launch.putExtra(AppReference.EXTRA_APPLICATION_ARGUMENT, content.optString(ARG));
	    }
	    // TODO: optimize!
	    List<ResolveInfo> resolved = context.getPackageManager().queryIntentActivities(launch, 0);
	    for (ResolveInfo r : resolved) {
	        ActivityInfo activity = r.activityInfo;
	        if (activity.packageName.equals(app.pkg())) {
	            launch.setClassName(activity.packageName, activity.name);
	            launch.putExtra("mobisocial.db.PACKAGE", activity.packageName);
	            if (content.has(STATE)) {
	                launch.putExtra("mobisocial.db.STATE", content.optString(STATE));
	            }
	            context.startActivity(launch);
	            return;
	        }
	    }

        Iterator keyIter = content.keys();

        while(keyIter.hasNext())
        {
            Log.d(TAG, keyIter.next().toString());
        }

        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+content.optString("packageName")));
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
        i.removeCategory("android.intent.category.LAUNCHER");
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
                                    launch.setComponent(new ComponentName(
                                                            info.packageName,
                                                            info.name));
                                    launch.putExtra("creator", true);
                                    launch.putExtra(
                                        "android.intent.extra.APPLICATION_ARGUMENT",
                                        arg);
                                    callback.onAppSelected(info.packageName, arg, launch);
                                }
                                else{
                                    Toast.makeText(context, 
                                                   "No applications found.",
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

	public interface Callback {
	    public void onAppSelected(String pkg, String arg, Intent localLaunch);
	}
}
