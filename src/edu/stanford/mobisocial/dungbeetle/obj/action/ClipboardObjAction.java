
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.feed.action.ClipboardAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import android.content.Context;
import android.widget.Toast;

public class ClipboardObjAction extends ObjAction {
    public void onAct(Context context, DbEntryHandler objType, JSONObject objData) {
        ClipboardAction.copyToClipboard(objType.getType(), objData);
        Toast.makeText(context, "Copied object to clipboard.", Toast.LENGTH_SHORT).show();
    }
}
