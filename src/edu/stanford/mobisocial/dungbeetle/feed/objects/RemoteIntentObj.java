package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Obj to update user profiles. Globally defined user attributes
 * are also scanned across all Objs.
 * {@see Contact#ATTR_LAN_IP}
 * {@see DbContactAttributes}
 */
public class RemoteIntentObj extends DbEntryHandler {
	public static final String TAG = "RemoteIntentObj";

    public static final String TYPE = "remoteintent";
    public static final String INTENT = "intent";
    public static final String EXTRAS = "extras";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(String intent, Bundle b){
        JSONObject obj = new JSONObject();
        try{
            obj.put(INTENT, intent);
            JSONObject extras = new JSONObject();
            for(String key : b.keySet()) {
            	extras.put(key, b.get(key));
            }
            obj.put(EXTRAS, extras);
            
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		String intent = obj.optString(INTENT);

		Intent i = new Intent();
		i.setAction(intent);
		
		try {
			JSONObject extras = new JSONObject(obj.optString(EXTRAS));
			Bundle b = new Bundle();
			Iterator itr = extras.keys();
			while(itr.hasNext()) {
				String key = (String)itr.next();
				b.putString(key, extras.optString(key));
			}
			
			i.putExtras(b);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		context.sendBroadcast(i);
	}
}