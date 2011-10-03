
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Opens the given Obj using its {@link Activator}.
 *
 */
public class RelatedObjAction extends ObjAction {

    @Override
    public void onAct(Context context, Uri feedUri, DbEntryHandler objType, long hash, JSONObject objData, byte[] raw) {
        // TODO:
        /*Intent viewComments = new Intent(Intent.ACTION_VIEW);
        viewComments.setDataAndType(objUri, DbObject.MIME_TYPE);
        mmContext.startActivity(viewComments);*/
        Uri objUri = feedUri.buildUpon().encodedPath(feedUri.getPath() + ":" + hash).build();

        Intent objViewActivity = new Intent(Intent.ACTION_VIEW);
        objViewActivity.setDataAndType(objUri, Feed.MIME_TYPE);
        context.startActivity(objViewActivity);
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        return MusubiBaseActivity.isDeveloperModeEnabled(context);
    }

    @Override
    public String getLabel(Context context) {
        return "See Related";
    }
}
