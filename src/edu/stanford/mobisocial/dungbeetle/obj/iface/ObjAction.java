
package edu.stanford.mobisocial.dungbeetle.obj.iface;

import org.json.JSONObject;

import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public abstract class ObjAction {
    public abstract String getLabel();

    public abstract void onAct(Context context, DbEntryHandler objType, JSONObject objData);

    public final void actOn(Context context, DbEntryHandler objType, JSONObject objData) {
        onAct(context, objType, objData);
    }

    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        return true;
    }
}
