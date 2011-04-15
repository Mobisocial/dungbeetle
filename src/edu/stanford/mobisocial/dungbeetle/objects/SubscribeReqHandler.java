package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.content.Context;

import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class SubscribeReqHandler extends MessageHandler{
	public SubscribeReqHandler(Context c) {
		super(c);
	}
    public boolean willHandle(Contact from, JSONObject msg){ 
        return msg.optString("type").equals("subscribe_req");
    }
    public void handleReceived(Contact from, JSONObject obj){
        Helpers.insertSubscriber(
            mContext, 
            from.id,
            obj.optString("subscribeToFeed"));
    }
}