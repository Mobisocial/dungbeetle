package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.method.BaseMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

public class LinkObj extends DbEntryHandler implements FeedRenderer, Activator {
    private static final String TAG = "dungbeetle";

    public static final String TYPE = "send_file";
    public static final String URI = "uri";
    public static final String MIME_TYPE = "mimeType";
    public static final String TITLE = "title";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String uri, String mimeType, String title) {
        return new DbObject(TYPE, json(uri, mimeType, title));
    }

    public static JSONObject json(String uri, String mimeType, String title) {
        JSONObject obj = new JSONObject();
        try{
            obj.put(MIME_TYPE, mimeType);
            obj.put(URI, uri);
            obj.put(TITLE, title);
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

	public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        String title;
        if (content.has(TITLE)) {
            title = "Link: " + content.optString(TITLE);
        } else {
            title = content.optString(URI);
        }
        valueTV.setText(title);
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        if(Linkify.addLinks(valueTV, Linkify.ALL)) {
            if(!allowInteractions)
            	valueTV.setMovementMethod(null);
        }
        	
        frame.addView(valueTV);
    }
	static final Pattern p = Pattern.compile("\\b[-0-9a-zA-Z+\\.]+:\\S+");
    @Override
    public void activate(Context context, long contactId, JSONObject content, byte[] raw) {
    	//linkify should have picked it up already but if we are in TV mode we
    	//still need to activate
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String text = content.optString(URI);
        //some shared links come in with two lines of text "title\nuri"
        //for example google maps does this and passes that same value as both the
        //uri and title
        
        //launch the first thing that looks like a link
        Matcher m = p.matcher(text);
        while(m.find()) {
	        Uri uri = Uri.parse(m.group());
	        String scheme = uri.getScheme();
	
	        if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
	            String type = content.optString(MIME_TYPE);
	            if (type != null && type.length() > 0) {
	                intent.setDataAndType(uri, type);
	            } else {
	                intent.setData(uri);
	            }
	            if (!(context instanceof Activity)) {
	                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            }
	            try {
	            	context.startActivity(intent);
	            } catch (ActivityNotFoundException e) {
	            	String msg;
	            	if(type != null)
	            		msg = "A third party application that supports " + type + " is required.";
	            	else
	            		msg = "A third party application that supports " + uri.getScheme() + " is required.";
	            	Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	            }
	            return;
	        }
        }    
    }
}