package edu.stanford.mobisocial.dungbeetle.feed.iface;

import java.util.LinkedHashSet;

import android.content.Context;
import android.net.Uri;

public abstract class FeedPresence {
    public final LinkedHashSet<Uri> mActiveFeeds = new LinkedHashSet<Uri>();

    public abstract String getName();

    public final void setFeedPresence(Context context, Uri feed, boolean present) {
        if (present) {
            mActiveFeeds.add(feed);
        } else {
            mActiveFeeds.remove(feed);
        }
        onPresenceUpdated(context, feed, present);
    }

    protected abstract void onPresenceUpdated(Context context, Uri feed, boolean present);

    public final LinkedHashSet<Uri> getFeedsWithPresence() {
        return mActiveFeeds;
    }

    public boolean isPresent(Uri feedUri) {
        return mActiveFeeds.contains(feedUri);
    }

    @Override
    public String toString() {
        return getName();
    }
}
