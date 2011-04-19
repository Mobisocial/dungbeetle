package edu.stanford.mobisocial.dungbeetle.objects;
import org.json.JSONException;
import org.json.JSONObject;

public class SubscribeReqObj{

    public static final String TYPE = "subscribe_req";
    public static final String SUBSCRIBE_TO_FEED = "subscribeToFeed";

    public static JSONObject json(String feedName){
        JSONObject obj = new JSONObject();
        try{
            obj.put(SUBSCRIBE_TO_FEED, feedName);
        }catch(JSONException e){}
        return obj;
    }

}
