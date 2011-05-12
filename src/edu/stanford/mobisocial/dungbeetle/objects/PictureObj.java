package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.App;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;

import android.util.Base64;

public class PictureObj implements FeedRenderer {
	public static final String TAG = "PictureObj";

    public static final String TYPE = "picture";
    public static final String DATA = "data";

        
    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
        }catch(JSONException e){}
        return obj;
    }

	public boolean willRender(JSONObject object) { 
        return object.optString("type").equals(TYPE);
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
}
