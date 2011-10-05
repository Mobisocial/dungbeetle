package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.util.ArrayList;
import java.util.List;

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
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

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

    private void launchAppWithMembership(Context context, Uri feedUri, String pkg, String[] membership) {
        // Start new application feed:
        Group g = Group.create(context);
        Uri appFeedUri = Feed.uriForName(g.feedName);
        DbObject anchor = FeedAnchorObj.create(feedUri.getLastPathSegment());
        Helpers.sendToFeed(context, anchor, appFeedUri);

        // App reference in parent feed:
        DbObject obj = AppReferenceObj.forFixedMembership(pkg, membership, g.feedName, g.dynUpdateUri);
        Helpers.sendToFeed(context, obj, feedUri);

        // Kick it off locally:
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
                Intent launch = new Intent(mAction);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(mResolveInfo.activityInfo.packageName, mResolveInfo.activityInfo.name);
                long[] contactIds = data.getLongArrayExtra("contacts");

                /**
                 * TODO:
                 * 
                 * Identity Firewall Goes Here.
                 * Membership details can be randomized in one of many ways.
                 * The app (scrabble) may see games a set of gamers play together.
                 * The app may always see random ids
                 * The app may always see stable ids
                 * 
                 * Can also permute the cursor and member order.
                 */

                String[] participantIds = new String[contactIds.length + 1];
                
                participantIds[0] = App.instance().getLocalPersonId();
                try {
                    int i = 1;
                    for (long id : contactIds) {
                        Maybe<Contact> annoyingContact = Contact.forId(mContext, id);
                        Contact contact = annoyingContact.get();
                        participantIds[i++] = contact.personId;
                    }
                } catch (NoValError e) {
                    Log.e(TAG, "please, Please get rid of the maybe.");
                    Toast.makeText(mContext, "Error getting app membership.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send notice of the new application session, and join:
                launchAppWithMembership(mContext, mFeedUri, mResolveInfo.activityInfo.packageName, participantIds);
            }
        }
    }
}
