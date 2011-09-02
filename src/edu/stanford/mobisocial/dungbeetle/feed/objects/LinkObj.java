package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import static edu.stanford.mobisocial.dungbeetle.util.AndroidActivityHelpers.*;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkObj implements DbEntryHandler, FeedRenderer, Activator {
    private static final String TAG = "dungbeetle";

    public static final String TYPE = "send_file";
    public static final String URI = "uri";
    public static final String MIME_TYPE = "mimeType";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String uri, String mimeType) {
        return new DbObject(TYPE, json(uri, mimeType));
    }

    public static JSONObject json(String uri, String mimeType){
        JSONObject obj = new JSONObject();
        try{
            obj.put("mimeType", mimeType);
            obj.put("uri", uri);
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		String mimeType = obj.optString(MIME_TYPE);
		Uri uri = Uri.parse(obj.optString(URI));
		if (fileAvailable(mimeType, uri)) {
    		Intent i = new Intent();
    		i.setAction(Intent.ACTION_VIEW);
    		i.addCategory(Intent.CATEGORY_DEFAULT);
    		i.setType(mimeType);
    		i.setData(uri);
    		i.putExtra(Intent.EXTRA_TEXT, uri);
    
    		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
    		(new PresenceAwareNotify(context)).notify("New content from Musubi",
                "New Content from Musubi", mimeType + "  " + uri, contentIntent);
		} else {
		    Log.w(TAG, "Received file, failed to handle: " + uri);
		}
	}

	private boolean fileAvailable(String mimeType, Uri uri) {
	    return uri != null && uri.getScheme().startsWith("http");
	}

	public void render(Context context, ViewGroup frame, JSONObject content) {
        TextView valueTV = new TextView(context);
        valueTV.setText(content.optString(URI));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    @Override
    public void activate(Context context, JSONObject content){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String text = content.optString(URI);
        Uri uri = Uri.parse(text);
        String scheme = uri.getScheme();

        if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            intent.setData(Uri.parse(content.optString(URI)));
            context.startActivity(intent);
        }
    }
}