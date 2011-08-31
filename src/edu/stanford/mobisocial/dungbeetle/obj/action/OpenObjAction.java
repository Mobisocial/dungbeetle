
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import android.content.Context;

public class OpenObjAction extends ObjAction {

    @Override
    public void onAct(Context context, DbEntryHandler objType, JSONObject objData) {
        if (objType instanceof Activator) {
            ((Activator) objType).activate(context, objData);
        }
    }

    @Override
    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        return (objType instanceof Activator);
    }

    @Override
    public String getLabel() {
        return "Open";
    }
}
