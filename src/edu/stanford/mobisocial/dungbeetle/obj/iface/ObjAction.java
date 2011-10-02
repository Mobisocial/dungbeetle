
package edu.stanford.mobisocial.dungbeetle.obj.iface;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public abstract class ObjAction {
    public abstract String getLabel();

    public abstract void onAct(Context context, Uri feedUri,
            DbEntryHandler objType, JSONObject objData, byte[] raw);

    public final void actOn(Context context, Uri feedUri,
            DbEntryHandler objType, JSONObject objDat, byte[] raw) {
        onAct(context, feedUri, objType, objDat, raw);
    }

    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        return true;
    }
}
