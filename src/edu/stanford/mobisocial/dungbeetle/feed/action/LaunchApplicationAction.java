package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class LaunchApplicationAction implements FeedAction {

    @Override
    public String getName() {
        return "Application...";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        promptForApplication(context, new OnAppSelected() {
            @Override
            public void onAppSelected(String pkg, String arg, Intent localLaunch) {
                // Start new application feed:
                Group g = Group.create(context);
                Uri appFeedUri = Feed.uriForName(g.feedName);
                DbObject anchor = FeedAnchorObj.create(feedUri.getLastPathSegment());
                Helpers.sendToFeed(context, anchor, appFeedUri);

                // App reference in parent feed:
                DbObject obj = AppReferenceObj.from(pkg, arg, g.feedName, g.dynUpdateUri);
                Helpers.sendToFeed(context, obj, feedUri);

                localLaunch.putExtra(AppState.EXTRA_FEED_URI, appFeedUri);
                if (arg != null) {
                    localLaunch.putExtra(AppState.EXTRA_APPLICATION_ARGUMENT, arg);
                }
                localLaunch.putExtra(AppState.EXTRA_APPLICATION_PACKAGE, pkg);
                context.startActivity(localLaunch);
            }
        });
    }

    public static void promptForApplication(final Context context, final OnAppSelected callback) {
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
                i.setClassName(info.activityInfo.packageName, info.activityInfo.name);
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
                            launch.setComponent(new ComponentName(info.packageName, info.name));
                            launch.putExtra("creator", true);
                            launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
                            callback.onAppSelected(info.packageName, arg, launch);
                        } else {
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

    public interface OnAppSelected {
        public void onAppSelected(String pkg, String arg, Intent localLaunch);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
