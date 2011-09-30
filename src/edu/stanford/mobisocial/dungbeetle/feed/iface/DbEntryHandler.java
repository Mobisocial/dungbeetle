package edu.stanford.mobisocial.dungbeetle.feed.iface;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base interface for DungBeetle objects.
 */
public abstract class DbEntryHandler {
    public abstract String getType();
    public abstract void handleDirectMessage(Context context, Contact from, JSONObject msg);
	public abstract JSONObject mergeRaw(JSONObject objData, byte[] raw);
	public abstract Pair<JSONObject, byte[]> splitRaw(JSONObject json);

	public void afterDatabaseInsertion(JSONObject json) {
	    if (json.has(DbObjects.TARGET_HASH)) {
	        // TODO
	    }
	}

	/**
	 * Handles an object, and returns true to insert it in the database.
	 */
	public boolean handleObjFromNetwork(Context context, Contact contact, JSONObject obj) {
	    return true;
	}

	/**
	 * Return false to allow deletions of an obj after it has been sent.
	 */
	public boolean storeOutboundObj() {
	    return true;
	}
}