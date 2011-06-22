package edu.stanford.mobisocial.dungbeetle.objects.iface;
import android.content.Context;
import android.view.ViewGroup;
import org.json.JSONObject;

/**
 * An interface for {@link DbEntryHandler} objects that have a 
 * visible entry in a feed.
 */
public interface FeedRenderer {
	public void render(Context context, ViewGroup frame, JSONObject content);
}