package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfilePictureObj implements DbEntryHandler, FeedRenderer, Activator {
	public static final String TAG = "ProfilePictureObj";
    public static final String TYPE = "profilepicture";
    public static final String DATA = "data";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
            
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		byte[] data = Base64.decode(obj.optString(DATA), Base64.DEFAULT);
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.PICTURE, data);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
        App.instance().contactImages.invalidate(from.id);
	}

	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        String bytes = content.optString(DATA);
        App.instance().objectImages.lazyLoadImage(bytes.hashCode(), bytes, imageView);
        frame.addView(imageView);
	}

	@Override
    public void activate(Context context, JSONObject content){
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("b64Bytes", bytes);
        context.startActivity(intent); 
    }
}
