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
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import android.net.Uri;
import android.util.Log;

public class JoinNotificationObj implements DbEntryHandler, UnprocessedMessageHandler {
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
    public void handleReceived(final Context context, Contact from, JSONObject obj) {
    }

    @Override
    public void handleUnprocessed(final Context context, JSONObject obj) {
        if (DBG) Log.i(TAG, "Message to update group. ");
        String feedName = obj.optString("feedName");
        final Uri uri = Uri.parse(obj.optString(JoinNotificationObj.URI));
        final GroupProviders.GroupProvider h = GroupProviders.forUri(uri);
        DBHelper helper = new DBHelper(context);
        final IdentityProvider ident = new DBIdentityProvider(helper);
        Maybe<Group> mg = helper.groupByFeedName(feedName);
        try {
            // group exists already, load view
            final Group g = mg.get();

            new Thread(){
                public void run(){
                    h.handle(g.id, uri, context, ident, g.version, false);
                }
            }.start();
        }
        catch(Maybe.NoValError e) { }
    }
}
