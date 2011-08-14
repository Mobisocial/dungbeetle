package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import org.json.JSONObject;

/**
 * Metadata marking the beginning of a feed.
 */
public class FeedAnchorObj implements DbEntryHandler, FeedRenderer {

    public static final String TYPE = "feed-anchor";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject create() {
        return new DbObject(TYPE, json());
    }

    public static JSONObject json(){
        JSONObject obj = new JSONObject();
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, JSONObject content) {
        TextView valueTV = new TextView(context);
        valueTV.setText("This is only the beginning.");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
