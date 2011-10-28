package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Handles Obj content for unknown types.
 *
 */
public class UnknownObj extends DbEntryHandler {

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return "UNKNOWN";
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {   
        Log.w("musubi", "Received unknown obj: " + msg);
    }
}
