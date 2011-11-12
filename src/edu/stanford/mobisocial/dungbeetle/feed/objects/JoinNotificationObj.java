package edu.stanford.mobisocial.dungbeetle.feed.objects;
import mobisocial.socialkit.Obj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class JoinNotificationObj extends DbEntryHandler implements UnprocessedMessageHandler, FeedRenderer {
    private static final String TAG = "dbJoin";
    private static boolean DBG = false;
    public static final String TYPE = "join_notification";
    public static final String URI = "uri";

    @Override
    public String getType() {
        return TYPE;
    }


    public static DbObject from(String uri) {
        return new DbObject(TYPE, json(uri));
    }
    
    public static JSONObject json(String uri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(URI, uri);
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public void handleDirectMessage(final Context context, Contact from, JSONObject obj) {
    }

    @Override
    public Pair<JSONObject, byte[]> handleUnprocessed(final Context context, JSONObject obj) {
    	//QQQQQQ: do we use the join notification obj for the distributed group protocol?
    	//conceptually we should resend our profile when we add a new friend, but that was being done here...
    	//maybe there is something else we should do here.
		return null;
    }


    @Override
    public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText("I'm here!");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
