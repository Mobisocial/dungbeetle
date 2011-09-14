package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class StatusObj implements DbEntryHandler, FeedRenderer, Activator {

    public static final String TYPE = "status";
    public static final String TEXT = "text";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String status) {
        return new DbObject(TYPE, json(status));
    }

    public static JSONObject json(String status){
        JSONObject obj = new JSONObject();
        try{
            obj.put(TEXT, status);
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, JSONObject content, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText(content.optString(TEXT));
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
    public void activate(Context context, JSONObject content){
    	//linkify should have picked it up already but if we are in TV mode we
    	//still need to activate
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String text = content.optString(TEXT);
        
        //launch the first thing that looks like a link
        Matcher m = p.matcher(text);
        while(m.find()) {
	        Uri uri = Uri.parse(m.group());
	        String scheme = uri.getScheme();
	
	        if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                intent.setData(uri);
	            if (!(context instanceof Activity)) {
	                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            }
	            context.startActivity(intent);
	            return;
	        }
        }    
    }
}
