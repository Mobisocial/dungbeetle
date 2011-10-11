package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.NoNotify;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationObj extends DbEntryHandler implements FeedRenderer, Activator, NoNotify {
    public static final String TYPE = "loc";
    public static final String COORD_LAT = "lat";
    public static final String COORD_LONG = "lon";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(Location location) {
        return new DbObject(TYPE, json(location));
    }

    public static JSONObject json(Location location){
        JSONObject obj = new JSONObject();
        try{
            obj.put(COORD_LAT, location.getLatitude());
            obj.put(COORD_LONG, location.getLongitude());
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        NumberFormat df =  DecimalFormat.getNumberInstance();
        df.setMaximumFractionDigits(5);
        df.setMinimumFractionDigits(5);
        
        String msg = "I'm at " + 
        	df.format(content.optDouble(COORD_LAT)) +
        	", " +
        	df.format(content.optDouble(COORD_LONG));


        valueTV.setText(msg);
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    @Override
    public void activate(Context context, long contactId, JSONObject content, byte[] raw) {
        String loc = "geo:" + content.optDouble(COORD_LAT) + "," +
                content.optDouble(COORD_LONG) + "?z=17";
        Intent map = new Intent(Intent.ACTION_VIEW, Uri.parse(loc));
        context.startActivity(map);
    }
}
