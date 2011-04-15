package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public abstract class MessageHandler {
	Context mContext;
	public MessageHandler(Context c) {
		mContext = c;
	}
    public abstract boolean willHandle(Contact from, JSONObject msg);

    public abstract void handleReceived(Contact from, JSONObject msg);
    
    protected PresenceAwareNotify getPresenceAwareNotify() {
    	return new PresenceAwareNotify(mContext);
    }
}