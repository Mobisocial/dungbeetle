package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import org.json.JSONException;
import org.json.JSONObject;

public class StatusObj implements IncomingMessageHandler, FeedRenderer {

    public static final String TYPE = "status";
    public static final String TEXT = "text";

    public static JSONObject json(String status){
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", status);
        }catch(JSONException e){}
        return obj;
    }
	
    public boolean willHandle(Contact from, JSONObject msg){
        return msg.optString("type").equals(TYPE);
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        String status = obj.optString(TEXT);
        String id = Long.toString(from.id);
        ContentValues values = new ContentValues();
        values.put(Contact.STATUS, status);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{id});
    }

	public boolean willRender(JSONObject object) {
		return object.optString("type").equals(TYPE);
	}
    
    public void render(View frame, JSONObject content) {
    	String status = content.optString(TEXT);
        TextView bodyText = (TextView)frame.findViewById(R.id.body_text);
        bodyText.setText(status);
    }
}
