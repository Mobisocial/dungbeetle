
package edu.stanford.mobisocial.dungbeetle.obj.action;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

/**
 * Opens the given Obj using its {@link Activator}.
 *
 */
public class OpenObjAction extends ObjAction {

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        if (objType instanceof Activator) {
            ((Activator) objType).activate(context, null);
        }
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        return (objType instanceof Activator);
    }

    @Override
    public String getLabel(Context context) {
        return "Open";
    }
}
