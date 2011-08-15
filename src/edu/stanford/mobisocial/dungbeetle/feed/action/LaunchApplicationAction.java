package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
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
        AppReferenceObj.promptForApplication(context, new AppReferenceObj.Callback() {
            @Override
            public void onAppSelected(String pkg, String arg, Intent localLaunch) {
                // Start new application feed:
                Group g = Group.create(context);
                Uri appFeedUri = Feed.uriForName(g.feedName);
                DbObject anchor = FeedAnchorObj.create(feedUri.getLastPathSegment());
                Helpers.sendToFeed(context, anchor, appFeedUri);

                // App reference in parent feed:
                DbObject obj = new AppReference(pkg, arg, g.feedName, g.dynUpdateUri);
                Helpers.sendToFeed(context, obj, feedUri);

                localLaunch.putExtra(AppReference.EXTRA_FEED_URI, appFeedUri);
                if (arg != null) {
                    localLaunch.putExtra(AppReference.EXTRA_APPLICATION_ARGUMENT, arg);
                }
                localLaunch.putExtra(AppReference.EXTRA_APPLICATION_PACKAGE, pkg);
                context.startActivity(localLaunch);
            }
        });
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
