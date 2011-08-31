package edu.stanford.mobisocial.dungbeetle.feed.iface;
import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

/**
 * Interface for {@see DbEntryHandler} objects which can be clicked
 * on in a feed for deeper interaction.
 */
public interface Activator {
	public void activate(Context context, JSONObject content);
}
