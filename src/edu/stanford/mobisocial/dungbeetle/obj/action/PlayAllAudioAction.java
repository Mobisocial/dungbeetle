
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.Base64;

public class SetProfileObjAction extends ObjAction {
    public void onAct(Context context, DbEntryHandler objType, JSONObject objData) {
        String b64Bytes = objData.optString(PictureObj.DATA);
        byte[] data = Base64.decode(b64Bytes);
        Helpers.updatePicture(context, data);
        Toast.makeText(context, "Set profile picture.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getLabel() {
        return "Set as Profile";
    }

    @Override
    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }
        return false;
        //return (objType instanceof PictureObj);
    }
}
