package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import java.util.Iterator;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.content.Context;

public class Helpers {

    public static void insertSubscriber(final Context c, 
                                        Long contactId, 
                                        String feedName){
        ContentValues values = new ContentValues();
        values.put(Subscriber.CONTACT_ID, contactId);
        values.put("feed_name", feedName);
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/subscribers");
        c.getContentResolver().insert(url, values);
    }


    public static Uri insertContact(final Context c, String pubKeyStr, 
                                    String name, String email){
        ContentValues values = new ContentValues();
        values.put(Contact.PUBLIC_KEY, pubKeyStr);
        values.put(Contact.NAME, name);
        values.put(Contact.EMAIL, email);
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        return c.getContentResolver().insert(url, values);
    }
    
    public static void sendApplicationInvite(final Context c, 
                                             final Collection<Contact> contacts, 
                                             final String packageName, final String arg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put(InviteObj.PACKAGE_NAME, packageName);
            obj.put(InviteObj.ARG, arg);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, "invite_app_session");
        c.getContentResolver().insert(url, values);
    }

    public static void sendIM(final Context c, 
                              final Collection<Contact> contacts, 
                              final String msg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", msg);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, "instant_message");
        c.getContentResolver().insert(url, values);
    }

    public static void sendFile(final Context c, final Collection<Contact> contacts, 
                                final String mimeType,
                                final String uri){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("mimeType", mimeType);
            obj.put("uri", uri);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, "send_file");
        c.getContentResolver().insert(url, values);
    }

    public static void sendMultiPartyInvite(Context c, 
                                            Collection<Contact> contacts, 
                                            String feedName){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("feedName", feedName);
            JSONArray participants = new JSONArray();
            Iterator<Contact> it = contacts.iterator();
            while(it.hasNext()){
                String localId = "@c" + it.next().id;
                participants.put(participants.length(), localId);
            }
            obj.put("participants", participants);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, InviteObj.TYPE);
        c.getContentResolver().insert(url, values);
    }

    private static String buildAddresses(Collection<Contact> contacts){
        String to = "";
        Iterator<Contact> it = contacts.iterator();
        while(it.hasNext()){
            Contact c = it.next();
            if(it.hasNext()){
                to += c.id + ",";
            }
            else{
                to += c.id;
            }
        }
        return to;
    }

    public static void updateStatus(final Context c, final String status){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            	obj.put("text", status);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.TYPE, "status");
        c.getContentResolver().insert(url, values); 
    }

    public static void updateProfile(final Context c, final String name){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("name", name);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.TYPE, "profile");
        c.getContentResolver().insert(url, values);
    }

}
