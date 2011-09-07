package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

/**
 * Automatically launches some received objects.
 */
public class AutoActivateObjHandler extends ObjHandler {
    @Override
    public void handleObj(Context context, Uri feedUri, long contactId, long sequenceId,
            DbEntryHandler handler, JSONObject json) {
        if (handler instanceof Activator) {
            ((Activator)handler).activate(context, json);
        }
    }
}