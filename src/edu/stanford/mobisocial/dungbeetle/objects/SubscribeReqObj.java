package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import android.view.View;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import org.json.JSONException;
import org.json.JSONObject;

public class SubscribeReqObj implements IncomingMessageHandler, FeedRenderer {

    public static final String TYPE = "subscribe_req";
    public static final String SUBSCRIBE_TO_FEED = "subscribeToFeed";

    public static JSONObject json(String feedName){
        JSONObject obj = new JSONObject();
        try{
            obj.put(SUBSCRIBE_TO_FEED, feedName);
        }catch(JSONException e){}
        return obj;
    }


    public boolean willHandle(Contact from, JSONObject msg){ 
        return msg.optString("type").equals(TYPE);
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        Helpers.insertSubscriber(
            context, 
            from.id,
            obj.optString(SUBSCRIBE_TO_FEED));
    }

	public boolean willRender(JSONObject object) {
		return false;
	}
    
    public void render(View frame, JSONObject content) {}

}