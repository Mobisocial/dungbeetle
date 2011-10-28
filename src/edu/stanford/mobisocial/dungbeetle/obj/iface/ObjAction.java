
package edu.stanford.mobisocial.dungbeetle.obj.iface;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public abstract class ObjAction {
    public abstract String getLabel(Context context);

    public abstract void onAct(Context context, DbEntryHandler objType, DbObj obj);

    public final void actOn(Context context, DbEntryHandler objType, DbObj obj) {
        onAct(context, objType, obj);
    }

    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        return true;
    }
}
