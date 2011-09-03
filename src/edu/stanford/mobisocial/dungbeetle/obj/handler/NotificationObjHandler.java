package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.NoNotify;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.ui.FeedListActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class NotificationObjHandler extends ObjHandler {
    String TAG = "NotificationObjHandler";
    final DBHelper mHelper;
    public NotificationObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    @Override
    public void handleObj(Context context, Uri feedUri, long contactId,
            long sequenceId, DbEntryHandler typeInfo, JSONObject json) {
        if (contactId == Contact.MY_ID) {
            return;
        }

        if (typeInfo == null || !(typeInfo instanceof FeedRenderer)) {
            return;
        }

        if (typeInfo instanceof NoNotify) {
            return;
        }

        Maybe<Group> group = mHelper.groupForFeedName(feedUri.getLastPathSegment());
        if (group.isKnown()) {
            Intent launch = new Intent(Intent.ACTION_VIEW);
            launch.setClass(context, FeedListActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    launch, PendingIntent.FLAG_CANCEL_CURRENT);

            try {
                (new PresenceAwareNotify(context)).notify("New Musubi message",
                        "New Musubi message", "In " + ((Group) group.get()).name,
                        contentIntent);
            } catch (NoValError e) {
                Log.e(TAG, "No group while notifying for " + feedUri.getLastPathSegment());
            }
        }
    }
}
