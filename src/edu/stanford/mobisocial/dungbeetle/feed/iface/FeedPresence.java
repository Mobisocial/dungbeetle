package edu.stanford.mobisocial.dungbeetle.feed.iface;

import java.util.HashSet;
import java.util.Set;

import android.net.Uri;

public abstract class FeedPresence {
    public Set<Uri> mActiveFeeds = new HashSet<Uri>();

    public abstract String getName();

    public void setFeedPresence(Uri feed, boolean present) {
        if (present) {
            mActiveFeeds.add(feed);
        } else {
            mActiveFeeds.remove(feed);
        }
    }

    public Set<Uri> getFeedsWithPresence() {
        return mActiveFeeds;
    }
}
