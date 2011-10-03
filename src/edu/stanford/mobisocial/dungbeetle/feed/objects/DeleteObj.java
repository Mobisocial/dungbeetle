package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.method.BaseMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class DeleteObj extends DbEntryHandler implements FeedMessageHandler {
    private static final String TAG = "dungbeetle";

    public static final String TYPE = "delete";
    public static final String HASH = "hash";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(long hash) {
        return new DbObject(TYPE, json(hash));
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    public static JSONObject json(long hash) {
    	//TODO: obj should mention feed
        JSONObject obj = new JSONObject();
        try{
            obj.put(HASH, hash);
        }catch(JSONException e){}
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		long hash = obj.optLong(HASH);
		DBHelper dbh = DBHelper.getGlobal(context);
		try {
			if (dbh.getObjSenderId(hash) == -666) {
				dbh.markObjectAsDeleted(hash);
			}
			else {
				dbh.deleteObjByHash(from.id, hash);
			}
		} finally {
			dbh.close();
		}
	}
	@Override
	public void handleFeedMessage(Context context, Uri feedUri, long contactId,
			long sequenceId, String type, JSONObject obj) {
		long hash = obj.optLong(HASH);
		DBHelper dbh = DBHelper.getGlobal(context);
		try {
			if (dbh.getObjSenderId(hash) == -666) {
				dbh.markObjectAsDeleted(hash);
			}
			else {
				dbh.deleteObjByHash(feedUri.toString(),  hash);
			}
		} finally {
			dbh.close();
		}
		
	}
	@Override
	public boolean discardOutboundObj() {
		return true;
	}
}