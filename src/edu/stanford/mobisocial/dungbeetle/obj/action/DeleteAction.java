
package edu.stanford.mobisocial.dungbeetle.obj.action;

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.DeleteObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class DeleteAction extends ObjAction {

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        DBHelper dbh = DBHelper.getGlobal(context);
        try {
        	//TODO: do with content provider... this method ignore the 
        	//feed uri for now
            long hash = obj.getHash();
            Uri feedUri = obj.getContainingFeed().getUri();
        	if (hash == 0) {
        		Toast.makeText(context, "Message not yet sent.", Toast.LENGTH_SHORT).show();
        		return;
        	}
        	Helpers.sendToFeeds(context, DeleteObj.TYPE, DeleteObj.json(hash), new Uri[] { feedUri });
        	dbh.deleteObjByHash(feedUri, hash);
        } finally {
        	dbh.close();
        }
    }

    @Override
    public String getLabel(Context context) {
		return "Delete";
    }

}
