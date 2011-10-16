package edu.stanford.mobisocial.dungbeetle.feed.iface;

import mobisocial.socialkit.SignedObj;
import android.content.Context;

/**
 * Interface for {@see DbEntryHandler} objects which can be clicked
 * on in a feed for deeper interaction.
 */
public interface Activator {
	public void activate(Context context, SignedObj obj);
}
