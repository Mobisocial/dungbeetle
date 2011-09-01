package edu.stanford.mobisocial.dungbeetle.feed.iface;
import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base interface for DungBeetle objects.
 */
public interface DbEntryHandler {
    String getType();
    void handleDirectMessage(Context context, Contact from, JSONObject msg);
}