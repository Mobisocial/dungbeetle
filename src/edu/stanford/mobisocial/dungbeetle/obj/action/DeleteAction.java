
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.DeleteObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

public class DeleteAction extends ObjAction {
    private static final String TAG = "passItOn";
    private static JSONObject mJson;
    private static DbEntryHandler mType;
    private Context mContext;

    public void onAct(Context context, Uri feedUri, DbEntryHandler objType, long hash, JSONObject objData, byte[] raw) {
        mContext = context;
        DBHelper dbh = DBHelper.getGlobal(context);
        try {
        	//TODO: do with content provider... this method ignore the 
        	//feed uri for now
        	if(hash == 0) {
        		Toast.makeText(context, "Message not yet sent.", Toast.LENGTH_SHORT).show();
        		return;
        	}
        	Helpers.sendToFeeds(context, DeleteObj.TYPE, DeleteObj.json(hash), new Uri[] { feedUri });
        	dbh.deleteObjByHash(feedUri.toString(), hash);
        } finally {
        	dbh.close();
        }
    }

    @Override
    public String getLabel(Context context) {
		return "Delete";
    }

}
