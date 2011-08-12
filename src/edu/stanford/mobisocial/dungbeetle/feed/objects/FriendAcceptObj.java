package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;

public class FriendAcceptObj implements DbEntryHandler, UnprocessedMessageHandler {
    public static final String TYPE = "friend_accept";
    public static final String URI = "uri";

    @Override
    public String getType() {
        return TYPE;
    }


    public static DbObject from(Uri uri) {
        return new DbObject(TYPE, json(uri));
    }
    
    public static JSONObject json(Uri uri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(URI, uri.toString());
        } catch(JSONException e) { }
        return obj;
    }

    public void handleReceived(Context context, Contact from, JSONObject obj) {

    }


    /**
     * Inserts a friend into the list of contacts based on a received
     * DungBeetle message, typically sent in response to peer accepting
     * a friend request.
     */
    @Override
    public void handleUnprocessed(Context context, JSONObject msg) {
        Uri uri = Uri.parse(msg.optString(URI));
        // TODO: prompt instead of auto-acccept?
        FriendRequest.acceptFriendRequest(context, uri, true);
    }
}
