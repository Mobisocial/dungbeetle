package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

import org.json.JSONException;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import edu.stanford.mobisocial.dungbeetle.Helpers;

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
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    @Override
    public void handleDirectMessage(final Context context, Contact from, JSONObject obj) {
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

    @Override
    public Pair<JSONObject, byte[]> handleUnprocessed(final Context context, JSONObject obj) {
        if (DBG) Log.i(TAG, "Message to update group. ");
        String feedName = obj.optString("feedName");
        final Uri uri = Uri.parse(obj.optString(JoinNotificationObj.URI));
        final GroupProviders.GroupProvider h = GroupProviders.forUri(uri);
        DBHelper helper = DBHelper.getGlobal(context);
        final IdentityProvider ident = new DBIdentityProvider(helper);
        Maybe<Group> mg = helper.groupByFeedName(feedName);
        try {
            // group exists already, load view
            final Group g = mg.get();

            new Thread(){
                public void run(){
                    h.handle(g.id, uri, context, g.version, false);
                }
            }.start();
        }
        catch(Maybe.NoValError e) { }
        ident.close();
        Helpers.resendProfile(context);
        helper.close();
        return null;
    }


    @Override
    public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText("I'm here!");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
