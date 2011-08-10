package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import static edu.stanford.mobisocial.dungbeetle.util.AndroidActivityHelpers.*;
import org.json.JSONException;
import org.json.JSONObject;

public class FileObj implements DbEntryHandler, FeedRenderer {

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

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		String mimeType = obj.optString(MIME_TYPE);
		Uri uri = Uri.parse(obj.optString(URI));

		String action = obj.optString("action", "open");
		
        // Run in parallel, allow local side-effects
		if ("open".equals(action)) {
    		if (fileAvailable(mimeType, uri)) {
        		Intent i = new Intent();
        		i.setAction(Intent.ACTION_VIEW);
        		i.addCategory(Intent.CATEGORY_DEFAULT);
        		i.setType(mimeType);
        		i.setData(uri);
        		i.putExtra(Intent.EXTRA_TEXT, uri);
        
        		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i,
                    PendingIntent.FLAG_CANCEL_CURRENT);
        		(new PresenceAwareNotify(context)).notify("New Shared File...",
                    "New Shared File", mimeType + "  " + uri, contentIntent);
    		} else {
    		    setContext(context);
    		    toast("file not found!");
    		}
		}

		// Run in parallel, allow local side-effects
		if ("get".equals("action")) {
		    
		}

		if ("put".equals("action")) {
		    
		}
	}

	private boolean fileAvailable(String mimeType, Uri uri) {
	    return uri.getScheme().startsWith("http");
	}

	public void render(Context context, ViewGroup frame, JSONObject content){}
}