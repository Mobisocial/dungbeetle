package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class ProfileObj implements DbEntryHandler {
	public static final String TAG = "ProfileObj";

    public static final String TYPE = "profile";
    public static final String NAME = "name";

    @Override
    public String getType() {
        return TYPE;
    }
     
    public static JSONObject json(String name, String about){
        JSONObject obj = new JSONObject();
        try{
            obj.put("name", name);
            obj.put("about", about);
            
        }catch(JSONException e){}
        return obj;
    }

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		String name = obj.optString(NAME);
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.NAME, name);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
	}
}