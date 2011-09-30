package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.NoNotify;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfilePictureObj extends DbEntryHandler implements NoNotify {
	public static final String TAG = "ProfilePictureObj";
    public static final String TYPE = "profilepicture";
    public static final String DATA = "data";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(byte[] data){
        String encoded = FastBase64.encodeToString(data);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
            
        }catch(JSONException e){}
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public boolean handleObjFromNetwork(Context context, Contact from, JSONObject obj) {
		byte[] data = FastBase64.decode(obj.optString(DATA));
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.PICTURE, data);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
        App.instance().contactImages.invalidate(from.id);

        return false;
	}

	@Override
	public boolean discardOutboundObj() {
		return true;
	};

	/*public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
	    TextView textView = new TextView(context);
	    textView.setText("New profile picture:");
	    textView.setLayoutParams(CommonLayouts.FULL_WIDTH);
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(CommonLayouts.WRAPPED);
        String bytes = content.optString(DATA);
        App.instance().objectImages.lazyLoadImage(bytes.hashCode(), bytes, imageView);
        frame.addView(textView);
        frame.addView(imageView);
	}

	@Override
    public void activate(Context context, JSONObject content, byte[] raw){
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("b64Bytes", bytes);
        context.startActivity(intent); 
    }*/

	@Override
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {

    }
}
