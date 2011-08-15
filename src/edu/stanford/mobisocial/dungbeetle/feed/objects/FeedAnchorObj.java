package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Metadata marking the beginning of a feed.
 */
public class FeedAnchorObj implements DbEntryHandler {

    public static final String TYPE = "feed-anchor";
    public static final String PARENT_FEED_NAME = "parent";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject create(String parentFeedName) {
        return new DbObject(TYPE, json(parentFeedName));
    }

    public static JSONObject json(String parentFeedName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PARENT_FEED_NAME, parentFeedName);
        } catch (JSONException e) {}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){

    }
}
