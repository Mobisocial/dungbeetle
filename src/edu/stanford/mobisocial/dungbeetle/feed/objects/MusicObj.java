package edu.stanford.mobisocial.dungbeetle.feed.objects;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import org.json.JSONException;
import org.json.JSONObject;

public class MusicObj implements DbEntryHandler, FeedRenderer {

    public static final String TYPE = "music";
    public static final String ARTIST = "a";
    public static final String TRACK = "t";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String action, String number) {
        return new DbObject(TYPE, json(action, number));
    }

    public static JSONObject json(String artist, String number){
        JSONObject obj = new JSONObject();
        try{
            obj.put(ARTIST, artist);
            obj.put(TRACK, number);
        }catch(JSONException e){}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, JSONObject content) {
        TextView valueTV = new TextView(context);
        valueTV.setText(asText(content));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    private String asText(JSONObject obj) {
        StringBuilder status = new StringBuilder();
        String a = obj.optString(ARTIST);
        String b = obj.optString(TRACK);
        status.append(a).append(" - ").append(b);
        return status.toString();
    }
}
