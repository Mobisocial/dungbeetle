
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import android.content.Context;
import android.net.Uri;

/**
 * Opens the given Obj using its {@link Activator}.
 *
 */
public class OpenObjAction extends ObjAction {

    @Override
    public void onAct(Context context, Uri feedUri, DbEntryHandler objType, long hash, JSONObject objData, byte[] raw) {
        if (objType instanceof Activator) {
            ((Activator) objType).activate(context, objData, raw);
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
