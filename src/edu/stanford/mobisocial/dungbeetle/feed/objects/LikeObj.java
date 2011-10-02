package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class LikeObj extends DbEntryHandler implements FeedRenderer {
    private static final String TAG = "musubi";

    public static final String TYPE = "like_ref";

    public static DbObject forObj(Long targetHash) {
        return new DbObject(TYPE, json(targetHash));
    }

    private static JSONObject json(Long targetHash) {
        JSONObject json = new JSONObject();
        try {
            json.put(DbObjects.TARGET_HASH, targetHash);
        } catch (JSONException e) {
        }
        return json;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {

    }

    @Override
    public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
        return null;
    }

    @Override
    public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
        return null;
    }

    @Override
    public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw,
            boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText("Re: " + content.optString(DbObjects.TARGET_HASH) + ", Yea.");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}