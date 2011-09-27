package edu.stanford.mobisocial.dungbeetle.feed.objects;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

/**
 * Metadata marking the beginning of a feed.
 */
public class FeedAnchorObj implements DbEntryHandler, FeedMessageHandler {
    private static final String TAG = "musubi";
    public static final String TYPE = "feed-anchor";
    public static final String PARENT_FEED_NAME = "parent";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject create(String parentFeedName) {
        return new DbObject(TYPE, json(parentFeedName));
    }

	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}
    public static JSONObject json(String parentFeedName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PARENT_FEED_NAME, parentFeedName);
        } catch (JSONException e) {}
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

    public void handleFeedMessage(Context context, Uri feedUri, long contactId, long sequenceId,
            String type, JSONObject obj) {
        String parentFeedName = obj.optString(PARENT_FEED_NAME);
        if (parentFeedName == null) {
            Log.e(TAG, "anchor for feed, but no parent given");
            return;
        }

        Maybe<Group> parentGroup = Group.forFeedName(context, parentFeedName);
        if (!parentGroup.isKnown()) {
            Log.e(TAG, "No parent entry found for " + parentFeedName);
            return;
        }
        Long parentId = -1l;
        try {
            parentId = parentGroup.get().id;
        } catch (NoValError e) {
        }

        String feedName = feedUri.getLastPathSegment();
        Log.d(TAG, "Updating parent_feed_id for " + feedName);
        DBHelper mHelper = DBHelper.getGlobal(context);
        ContentValues cv = new ContentValues();
        cv.put(Group.PARENT_FEED_ID, parentId);
        mHelper.getWritableDatabase().update(Group.TABLE, cv, Group.FEED_NAME + "=?",
                new String[]{feedName});
        mHelper.close();
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {

    }
}
