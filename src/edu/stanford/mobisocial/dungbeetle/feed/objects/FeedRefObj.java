package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.Intent;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;

import android.net.Uri;
import android.util.Pair;

public class FeedRefObj extends DbEntryHandler implements FeedRenderer, Activator {

    public static final String TAG = "FeedObj";

    public static final String TYPE = "feed_ref";
    public static final String FEED_ID = "feed_id";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(Group g) {
        return Feed.forGroup(g);
    }

	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}
    public static JSONObject json(Group g){
        JSONObject obj = new JSONObject();
        try{
            obj.put(FEED_ID, g.feedName);
        }catch(JSONException e) { 
            e.printStackTrace();
        }
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
		TextView view = new TextView(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        String feedName = content.optString(FEED_ID);
        view.setText(feedName);
        view.setBackgroundColor(Feed.colorFor(feedName));
        frame.addView(view);
	}

	@Override
    public void activate(Context context, JSONObject content, byte[] raw){
	    Feed feedRef = new Feed(content);
	    Maybe<Group> mg = Group.forFeedName(context, feedRef.id());
	    try {
	        Group g = mg.get();
            Group.view(context, g);
	    } catch (NoValError e) {

        }
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {
        Toast.makeText(context, "received", 400).show();
    }

}
