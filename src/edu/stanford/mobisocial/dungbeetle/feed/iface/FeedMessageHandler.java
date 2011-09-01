package edu.stanford.mobisocial.dungbeetle.feed.iface;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;

/**
 * Handle messages received to a feed.
 */
public interface FeedMessageHandler {
    String getType();
    void handleFeedMessage(Context context, Uri feedUri,
            long contactId, long sequenceId, String type, JSONObject msg);
}