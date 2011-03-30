package edu.stanford.mobisocial.dungbeetle;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.content.Context;

public class Helpers {
    
    public static void sendApplicationInvite(final Context c, final Contact contact, 
                                             final String packageName, final String arg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("packageName", packageName);
            obj.put("arg", arg);
        }catch(JSONException e){}
        values.put("json", obj.toString());
        values.put("to_person_id", contact.personId);
        values.put("type", "invite");
        c.getContentResolver().insert(url, values);
    }

    public static void updateStatus(final Context c, final String status){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", status);
        }catch(JSONException e){}
        values.put("json", obj.toString());
        values.put("type", "status");
        c.getContentResolver().insert(url, values); 
    }

}
