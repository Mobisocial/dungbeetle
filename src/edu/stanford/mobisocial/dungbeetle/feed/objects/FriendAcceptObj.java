package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class FriendAcceptObj implements DbEntryHandler, UnprocessedMessageHandler {

    public static final String TYPE = "friend_accept";
    public static final String URI = "uri";

    @Override
    public String getType() {
        return TYPE;
    }


    public static DbObject from(String uri) {
        return new DbObject(TYPE, json(uri));
    }
    
    public static JSONObject json(String uri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(URI, uri);
        }catch(JSONException e){}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){

    }


    @Override
    public void handleUnprocessed(Context context, JSONObject msg) {
        
    }
}
