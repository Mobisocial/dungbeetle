
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.FeedHistoryActivity;

public class ViewFeedObjAction extends ObjAction {
    @Override
    public void onAct(Context context, DbEntryHandler objType, JSONObject objData) {
        if (objData.has(DbObject.CHILD_FEED_NAME)) {
            Uri appFeed = Feed.uriForName(objData.optString(DbObject.CHILD_FEED_NAME));
            Intent viewFeed = new Intent(context, FeedHistoryActivity.class);
            viewFeed.setDataAndType(appFeed, Feed.MIME_TYPE);
            context.startActivity(viewFeed);
        }
    }

    @Override
    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        return false; //objData.has(DbObject.CHILD_FEED_NAME);
    }

    @Override
    public String getLabel() {
        return "Show History";
    }
}
