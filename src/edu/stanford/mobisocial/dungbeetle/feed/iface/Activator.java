package edu.stanford.mobisocial.dungbeetle.feed.iface;
import org.json.JSONObject;

import android.content.Context;

/**
 * Interface for {@see DbEntryHandler} objects which can be clicked
 * on in a feed for deeper interaction.
 */
public interface Activator {
	public void activate(Context context, long contactId, JSONObject content,
	        byte[] raw);
}
