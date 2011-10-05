package edu.stanford.mobisocial.dungbeetle.feed.objects;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

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

    public static JSONObject json(long hash) {
    	//TODO: obj should mention feed
        JSONObject obj = new JSONObject();
        try{
            obj.put(HASH, hash);
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		long hash = obj.optLong(HASH);
		DBHelper dbh = DBHelper.getGlobal(context);
		try {
			if (dbh.getObjSenderId(hash) == Contact.MY_ID) {
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
			if (dbh.getObjSenderId(hash) == Contact.MY_ID) {
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