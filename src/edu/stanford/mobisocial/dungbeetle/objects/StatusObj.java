package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.objects.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class StatusObj implements DbEntryHandler, FeedRenderer {

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
            obj.put("text", status);
        }catch(JSONException e){}
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){
        String status = obj.optString(TEXT);
        String id = Long.toString(from.id);
        ContentValues values = new ContentValues();
        values.put(Contact.STATUS, status);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{id});
    }

    public void render(Context context, ViewGroup frame, JSONObject content) {
        TextView valueTV = new TextView(context);
        valueTV.setText(content.optString(TEXT));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
