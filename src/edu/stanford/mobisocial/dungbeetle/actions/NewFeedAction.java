package edu.stanford.mobisocial.dungbeetle.actions;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.actions.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.objects.FeedObj;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;

public class NewFeedAction implements FeedAction {
    @Override
    public String getName() {
        return "Feed";
    }

    public void onClick(Context context, Uri feedUri) {
        Group g = Group.create(context);
        Helpers.sendToFeed(context,
                StatusObj.from("Welcome to " + g.name + "!"), Feed.uriForName(g.feedName));
        Helpers.sendToFeed(context, FeedObj.from(g), feedUri);
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
