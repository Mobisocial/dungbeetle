package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.objects.iface.FeedRenderer;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;

import android.util.Base64;

public class PictureObj implements DbEntryHandler, FeedRenderer, Activator {
	public static final String TAG = "PictureObj";

    public static final String TYPE = "picture";
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
	
	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        String bytes = content.optString(DATA);
        App.instance().objectImages.lazyLoadImage(bytes.hashCode(), bytes, imageView);
        frame.addView(imageView);
	}

    public void activate(Context context, JSONObject content){
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("b64Bytes", bytes);
        context.startActivity(intent); 
    }

    @Override
    public void handleReceived(Context context, Contact from, JSONObject msg) {
        // TODO Auto-generated method stub
        
    }

}
