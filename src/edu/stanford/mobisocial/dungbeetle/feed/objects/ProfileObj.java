package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.util.UUID;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.MemObj;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Obj to update user profiles. Globally defined user attributes
 * are also scanned across all Objs.
 * {@see Contact#ATTR_LAN_IP}
 * {@see DbContactAttributes}
 */
public class ProfileObj extends DbEntryHandler {
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

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		String name = obj.optString(NAME);
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.NAME, name);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
	}

	/**
	 * Returns an Obj of type {@link #TYPE} with a profile representing
	 * the local user. The profile is set with the given name and info fields.
	 */
    public static Obj forLocalUser(Context c, String name, String about) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", name);
            obj.put("about", about);

            // TODO: Framework.
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter != null) {
                UUID btUuid = ContentCorral.getLocalBluetoothServiceUuid(c);
                String btMac = btAdapter.getAddress();
                obj.put(Contact.ATTR_BT_MAC, btMac);
                obj.put(Contact.ATTR_BT_CORRAL_UUID, btUuid.toString());
            }
            obj.put(Contact.ATTR_PROTOCOL_VERSION, App.POSI_VERSION);
        }catch(JSONException e){}
        return new MemObj(TYPE, obj);
    }
}