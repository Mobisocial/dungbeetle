package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.Base64;

public class SharedSecretObj implements DbEntryHandler {

    public static final String TYPE = "shared_secret";
    public static final String RAW = "raw";
    public static final SecureRandom random = new SecureRandom();

    public static byte[] getOrPushSecret(Context context, Contact other) {
    	if(other.secret != null) {
    		return other.secret;
    	}
        ContentValues values = new ContentValues();
        byte[] ss = new byte[32];
        random.nextBytes(ss);
        values.put(Contact.SHARED_SECRET, ss);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{String.valueOf(other.id)});
        Helpers.sendMessage(context, other, new DbObject(TYPE, json(ss)));
        return ss;
    }
    		
    public static JSONObject json(byte[] shared_secret){
        JSONObject obj = new JSONObject();
        try{
            obj.put(RAW, Base64.encodeToString(shared_secret, false));
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        String raw_b64;
		try {
			raw_b64 = obj.getString(RAW);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
        byte[] ss = Base64.decode(raw_b64);
        if(from.secret != null && new BigInteger(from.secret).compareTo(new BigInteger(ss)) > 0) {
        	//ignore the new key according to a time independent metric...
        	return;
        }

        ContentValues values = new ContentValues();
        random.nextBytes(ss);
        values.put(Contact.SHARED_SECRET, ss);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{String.valueOf(from.id)});
    }
    
}
