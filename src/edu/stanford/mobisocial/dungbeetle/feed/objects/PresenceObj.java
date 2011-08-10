package edu.stanford.mobisocial.dungbeetle.feed.objects;

import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;


public class PresenceObj implements DbEntryHandler {

    public static final String TYPE = "presence";
    public static final String PRESENCE = "presence";

    public static JSONObject json(int presence){
        JSONObject obj = new JSONObject();
        try{
            obj.put("presence", presence);
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        int presence = obj.optInt(PRESENCE);
        String id = Long.toString(from.id);
        long time = obj.optLong(DbObject.TIMESTAMP);
        ContentValues values = new ContentValues();
        values.put(Contact.PRESENCE, presence);
        values.put(Contact.LAST_PRESENCE_TIME, time);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{id});
    }
}