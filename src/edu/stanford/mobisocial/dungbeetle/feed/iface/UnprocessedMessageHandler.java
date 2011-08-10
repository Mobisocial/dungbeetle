package edu.stanford.mobisocial.dungbeetle.feed.iface;
import android.content.Context;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base interface for DungBeetle objects.
 */
public interface UnprocessedMessageHandler {
    String getType();
    void handleUnprocessed(Context context, JSONObject msg);
}