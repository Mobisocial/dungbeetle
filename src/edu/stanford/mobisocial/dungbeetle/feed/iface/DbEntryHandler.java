package edu.stanford.mobisocial.dungbeetle.feed.iface;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base interface for DungBeetle objects.
 */
public abstract class DbEntryHandler {
    public abstract String getType();
    public abstract void handleDirectMessage(Context context, Contact from, JSONObject msg);
	public abstract JSONObject mergeRaw(JSONObject objData, byte[] raw);
	public abstract Pair<JSONObject, byte[]> splitRaw(JSONObject json);
}