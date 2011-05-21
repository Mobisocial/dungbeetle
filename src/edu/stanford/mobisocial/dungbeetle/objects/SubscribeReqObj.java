package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class SubscribeReqObj implements DbEntryHandler {

    public static final String TYPE = "subscribe_req";
    public static final String SUBSCRIBE_TO_FEED = "subscribeToFeed";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(String feedName){
        JSONObject obj = new JSONObject();
        try{
            obj.put(SUBSCRIBE_TO_FEED, feedName);
        }catch(JSONException e){}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        Helpers.insertSubscriber(
            context, 
            from.id,
            obj.optString(SUBSCRIBE_TO_FEED));
    }
}