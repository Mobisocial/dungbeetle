package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;

import org.json.JSONException;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.net.Uri;

public class JoinNotificationObj implements DbEntryHandler {

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

    public void handleReceived(Context context, Contact from, JSONObject obj){
        Uri uri = Uri.parse(obj.optString(URI));
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        String feedName = gp.feedName(uri);
        DBHelper helper = new DBHelper(context);
        DBIdentityProvider ident = new DBIdentityProvider(helper);
        Maybe<Group> mg = helper.groupByFeedName(feedName);
        long id = -1;
        try{
            // group exists already, load view
            Group g = mg.get();
            id = g.id;
            int version = -1;
            gp.forceUpdate(id, uri, context, ident, version);
        }
        catch(Maybe.NoValError e){
            // group does not exist yet, time to prompt for join

        }
        
        ident.close();
        helper.close();
        
    }
}
