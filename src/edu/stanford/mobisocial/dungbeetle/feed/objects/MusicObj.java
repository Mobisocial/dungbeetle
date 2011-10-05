package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

public class MusicObj extends DbEntryHandler implements FeedRenderer, Activator {

    public static final String TYPE = "music";
    public static final String ARTIST = "a";
    public static final String ALBUM = "l";
    public static final String TRACK = "t";
    public static final String URL = "url";
    public static final String MIME_TYPE = "mimeType";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String action, String number) {
        return new DbObject(TYPE, json(action, number));
    }

    public static DbObject from(String artist, String album, String track) {
        return new DbObject(TYPE, json(artist, album, track));
    }

    public static JSONObject json(String artist, String number) {
        JSONObject obj = new JSONObject();
        try{
            obj.put(ARTIST, artist);
            obj.put(TRACK, number);
        }catch(JSONException e){}
        return obj;
    }

    public static JSONObject json(String artist, String album, String track) {
        JSONObject obj = new JSONObject();
        try{
            obj.put(ARTIST, artist);
            obj.put(ALBUM, album);
            obj.put(TRACK, track);
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
        LinearLayout container = new LinearLayout(context);
        container.setLayoutParams(CommonLayouts.FULL_WIDTH);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.play);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView valueTV = new TextView(context);
        valueTV.setText(asText(content));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        valueTV.setPadding(4, 0, 0, 0);

        container.addView(imageView);
        container.addView(valueTV);
        frame.addView(container);
    }

    private String asText(JSONObject obj) {
        StringBuilder status = new StringBuilder();
        String a = obj.optString(ARTIST);
        String b = obj.optString(TRACK);
        if (b == null || b.length() == 0) {
            b = obj.optString(ALBUM);
        }
        status.append(a).append(" - ").append(b);
        return status.toString();
    }

    @Override
    public void activate(Context context, long contactId, JSONObject content, byte[] raw) {
        if (content.has(URL)) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(content.optString(URL));
            String type = "audio/x-mpegurl";
            if (content.has(MIME_TYPE)) {
                type = content.optString(MIME_TYPE);
            }
            view.setDataAndType(uri, type);
            if (!(context instanceof Activity)) {
                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(view);
        }
    }
}
