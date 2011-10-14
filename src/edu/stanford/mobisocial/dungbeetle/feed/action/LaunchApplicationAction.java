package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.multiplayer.Multiplayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

public class LaunchApplicationAction implements FeedAction {

    public static final String TAG = "LaunchApplicationAction";
    public static final boolean DBG = true;

    @Override
    public String getName() {
        return "Application...";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        promptForApplication(context, feedUri);
    }

    private void promptForApplication(final Context context, final Uri feedUri) {
        final PackageManager mgr = context.getPackageManager();
        final List<ResolveInfo> availableAppInfos = new ArrayList<ResolveInfo>();
        Intent i = new Intent();

        /** multiplayer applications **/
        ArrayList<String> intentActions = new ArrayList<String>();
        List<ResolveInfo> infos;
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        i.setAction(Multiplayer.ACTION_MULTIPLAYER);        
        infos = mgr.queryIntentActivities(i, 0);
        if (DBG) Log.d(TAG, "Queried " + infos.size() + "multiplayer apps.");
        for (int j = 0; j < infos.size(); j++) {
            intentActions.add(Multiplayer.ACTION_MULTIPLAYER);
        }
        availableAppInfos.addAll(infos);

        i.setAction(Multiplayer.ACTION_TWO_PLAYERS);
        infos = mgr.queryIntentActivities(i, 0);
        if (DBG) Log.d(TAG, "Queried " + infos.size() + "two-player apps.");
        for (int j = 0; j < infos.size(); j++) {
            intentActions.add(Multiplayer.ACTION_TWO_PLAYERS);
        }
        availableAppInfos.addAll(infos);

        final int numNPlayer = availableAppInfos.size();

        /** Negotiate p2p connectivity via receivers **/
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
        final String[] actions = intentActions.toArray(new String[]{});
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share application:");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                final ResolveInfo info = availableAppInfos.get(item);
                Intent i = new Intent();
                i.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                if (item < numNPlayer) {
                    ((InstrumentedActivity)context).doActivityForResult(
                            new MembersSelectedCallout(context, feedUri, actions[item], info));
                    return;
                } else {
                    i.setAction("android.intent.action.CONFIGURE");
                    i.addCategory("android.intent.category.P2P");
                }

                BroadcastReceiver rec = new BroadcastReceiver() {
                    public void onReceive(Context c, Intent i) {
                        Intent launch = new Intent();
                        launch.setAction(Intent.ACTION_MAIN);
                        launch.addCategory(Intent.CATEGORY_LAUNCHER);
                        launch.setPackage(info.activityInfo.packageName);
                        List<ResolveInfo> resolved =
                            mgr.queryIntentActivities(launch, 0);
                        if (resolved.size() > 0) {
                            ActivityInfo info = resolved.get(0).activityInfo;
                            String arg = getResultData();
                            launchAppWithArgument(context, feedUri, info.packageName, arg);
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
        if (context instanceof InstrumentedActivity) {
            ((InstrumentedActivity)context).showDialog(alert);
        } else {
            alert.show();
        }
    }

    public interface OnAppSelected {
        public void onAppSelected(String pkg, String arg, Intent localLaunch);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private void launchAppWithArgument(Context context, Uri feedUri, String pkg, String arg) {
        // Start new application feed:
        Group g = Group.create(context);
        Uri appFeedUri = Feed.uriForName(g.feedName);
        DbObject anchor = FeedAnchorObj.create(feedUri.getLastPathSegment());
        Helpers.sendToFeed(context, anchor, appFeedUri);

        // App reference in parent feed:
        DBHelper mHelper = DBHelper.getGlobal(context);
        DBIdentityProvider mIdent = new DBIdentityProvider(mHelper);

        long creatorId = mIdent.userPublicKeyString().hashCode();
        DbObject obj = AppReferenceObj.from(pkg, arg, g.feedName, g.dynUpdateUri, creatorId);
        Helpers.sendToFeed(context, obj, feedUri);

        mIdent.close();
        mHelper.close();

        new AppReferenceObj().activate(context, Contact.MY_ID, obj.getJson(), null);
    }

    /**
     * Callout used to select members for a new mutliplayer session
     * and then launch the application upon choosing them.
     */
    class MembersSelectedCallout implements ActivityCallout {
        private final Context mContext;
        private final Uri mFeedUri;
        private final String mAction;
        private final ResolveInfo mResolveInfo;

        public MembersSelectedCallout(Context context, Uri feedUri, String action, ResolveInfo info) {
            mFeedUri = feedUri;
            mContext = context;
            mAction = action;
            mResolveInfo = info;
        }

        @Override
        public Intent getStartIntent() {
            Intent i = new Intent(PickContactsActivity.INTENT_ACTION_PICK_CONTACTS);
            if (Multiplayer.ACTION_TWO_PLAYERS.equals(mAction)) {
                i.putExtra(PickContactsActivity.INTENT_EXTRA_MEMBERS_MAX, 1);
            }
            i.putExtra(PickContactsActivity.INTENT_EXTRA_PARENT_FEED, mFeedUri);
            return i;
        }

        @Override
        public void handleResult(int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                String action = mAction;
                String pkgName = mResolveInfo.activityInfo.packageName;
                String className = mResolveInfo.activityInfo.name;

                // Create and share new application instance
                DbObject obj = AppObj.fromPickerResult(mContext, action, pkgName, className, data);
                Uri objUri = Helpers.sendToFeed(mContext, obj, mFeedUri);
                mContext.getContentResolver().registerContentObserver(objUri, false,
                        new ObjObserver(mContext, objUri, new Handler(mContext.getMainLooper())));
            }
        }
    }

    private class ObjObserver extends ContentObserver {
        private final Uri mUri;
        private final Context mContext;
        public ObjObserver(Context context, Uri uri, Handler h) {
            super(h);
            mUri = uri;
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "NOTICED THAT WE NOW HAVE AN ENCODING FOR " + mUri);
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    private void launchLocally(DbObject obj, Context context) {
        DbEntryHandler h = DbObjects.forType(AppObj.TYPE);
        if (!(h instanceof Activator)) {
            Log.e(TAG, "What! " + AppObj.TYPE + " isn't an activator!");
            return;
        }
        ((Activator)h).activate(context, Contact.MY_ID, obj.getJson(), null);
    }
}
