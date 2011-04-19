package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;


public class PresenceHandler extends MessageHandler{
	public PresenceHandler(Context c) {
		super(c);
	}
    public boolean willHandle(Contact from, JSONObject msg){
        return msg.optString("type").equals("presence");
    }
    public void handleReceived(Contact from, JSONObject obj){
        int presence = Integer.parseInt(obj.optString("presence"));
        String id = Long.toString(from.id);
        
        ContentValues values = new ContentValues();
        values.put(Contact.PRESENCE, presence);
        mContext.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), values, "_id=?", new String[]{id});
    }
}