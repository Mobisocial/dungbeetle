package edu.stanford.mobisocial.dungbeetle.feed.iface;
import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;

/**
 * Handle messages received to a feed.
 */
public interface FeedMessageHandler {
    String getType();
    void handleFeedMessage(Context context, DbObj obj);
}