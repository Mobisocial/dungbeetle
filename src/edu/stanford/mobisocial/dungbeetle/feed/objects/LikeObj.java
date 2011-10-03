package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class LikeObj extends DbEntryHandler {
    private static final String TAG = "musubi";

    public static final String LABEL = "label";
    public static final String TYPE = "like_ref";

    public static DbObject forObj(Long targetHash) {
        return new DbObject(TYPE, json(targetHash));
    }

    public static DbObject forObj(Long targetHash, String badge) {
        return new DbObject(TYPE, json(targetHash, badge));
    }

    private static JSONObject json(Long targetHash) {
        JSONObject json = new JSONObject();
        try {
            json.put(DbObjects.TARGET_HASH, targetHash);
        } catch (JSONException e) {
        }
        return json;
    }

    private static JSONObject json(Long targetHash, String label) {
        JSONObject json = new JSONObject();
        try {
            json.put(DbObjects.TARGET_HASH, targetHash);
            json.put(LABEL, label);
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
}