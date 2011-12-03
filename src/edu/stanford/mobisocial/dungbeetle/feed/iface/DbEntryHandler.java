package edu.stanford.mobisocial.dungbeetle.feed.iface;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base interface for DungBeetle objects.
 */
public abstract class DbEntryHandler {
    public abstract String getType();

    /**
     * Handle a message that has been received from the network and is
     * intended for the local user directly.
     */
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {
        
    }

    public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
        return objData;
    }

    public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
        return null;
    }

	/**
	 * Handles an object, and returns true to insert it in the database.
	 */
	public boolean handleObjFromNetwork(Context context, Contact contact, JSONObject obj) {
	    return true;
	}

	/**
	 * Return true to allow deletions of an obj after it has been sent.
	 */
	public boolean discardOutboundObj() {
	    return false;
	}

	public boolean doNotification(Context context, DbObj obj) {
	    return true;
	}
}