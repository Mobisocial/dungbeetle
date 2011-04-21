package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.R;

import android.util.Base64;

public class ProfilePictureObj implements IncomingMessageHandler, FeedRenderer {
	public static final String TAG = "ProfilePictureObj";

    public static final String TYPE = "profilepicture";
    public static final String DATA = "data";

        
    public static JSONObject json(String data){
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", data);
            
        }catch(JSONException e){}
        return obj;
    }

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		byte[] data = Base64.decode(obj.optString(DATA), Base64.DEFAULT);
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.PICTURE, data);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
	}

	public boolean willRender(JSONObject object) { 
		return willHandle(null, object);
	}

	public void render(Context context, ViewGroup frame, JSONObject content) {
		TextView valueTV = new TextView(context);
        valueTV.setText("PROFILE PICTURE!!!");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
	}

}
