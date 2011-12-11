package edu.stanford.mobisocial.dungbeetle.obj.handler;

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

/**
 * Automatically launches some received objects.
 */
public class AutoActivateObjHandler extends ObjHandler {
    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        if (!context.getSharedPreferences("main", 0).getBoolean("autoplay", false)) {
            return;
        }
        // Don't activate subfeed items
        if (obj.getJson() != null && obj.getJson().has(DbObjects.TARGET_HASH)) {
            return;
        }
        if (handler instanceof Activator) {
            ((Activator)handler).activate(context, obj);
        }
    }
}
