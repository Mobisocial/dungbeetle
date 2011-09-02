package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.ui.FeedListActivity;

public class NotificationObjHandler extends ObjHandler {
    String TAG = "NotificationObjHandler";

    @Override
    public void handleObj(Context context, Uri feedUri, long contactId,
            long sequenceId, DbEntryHandler typeInfo, JSONObject json) {
        if (contactId == Contact.MY_ID) {
            return;
        }

        if (typeInfo == null || !(typeInfo instanceof FeedRenderer)) {
            return;
        }

        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setClass(context, FeedListActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                launch, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
