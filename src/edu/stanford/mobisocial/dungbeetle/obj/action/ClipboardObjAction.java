
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.action.ClipboardAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class ClipboardObjAction extends ObjAction {
    public void onAct(Context context, DbEntryHandler objType, JSONObject objData, byte[] raw) {
    	objData = objType.mergeRaw(objData, raw);
        ClipboardAction.copyToClipboard(context, objType.getType(), objData);
        Toast.makeText(context, "Copied object to clipboard.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getLabel() {
        return "Copy to Clipboard";
    }
}
