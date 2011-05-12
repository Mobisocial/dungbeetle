package edu.stanford.mobisocial.dungbeetle.objects;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.json.JSONException;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

import android.util.Base64;

public class ProfilePictureObj implements IncomingMessageHandler, FeedRenderer {
	public static final String TAG = "ProfilePictureObj";
    public static final String TYPE = "profilepicture";
    public static final String DATA = "data";

        
    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
            
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
		ImageView imageView = new ImageView(context);
		byte[] data = Base64.decode(content.optString(DATA), Base64.DEFAULT);
		imageView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(imageView);
	}

}
