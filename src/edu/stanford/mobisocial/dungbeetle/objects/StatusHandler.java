package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;


//figure out what to name this stuff
public class StatusHandler extends MessageHandler implements Renderable {
	
	public StatusHandler(Context c) {
		super(c);
	}
    public boolean willHandle(Contact from, JSONObject msg){
        return msg.optString("type").equals("status");
    }
    public void handleReceived(Contact from, JSONObject obj){
        String status = obj.optString("text");
        String id = Long.toString(from.id);
        
        ContentValues values = new ContentValues();
        values.put(Contact.STATUS, status);
        mContext.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), values, "_id=?", new String[]{id});
    }
    
    public void renderToFeed(View frame, JSONObject content) {
    	String status = content.optString("text");
        TextView bodyText = (TextView)frame.findViewById(R.id.body_text);
        bodyText.setText(status);
    }
}
