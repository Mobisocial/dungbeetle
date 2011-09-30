package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Date;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;

/**
 * Updates the database's "last modified" fields.
 *
 */
public class FeedModifiedObjHandler extends DatabaseObjHandler {

    public FeedModifiedObjHandler(DBHelper helper) {
        super(helper);
    }

    @Override
    public void handleObj(Context context, Uri feedUri, DbEntryHandler typeInfo,
            JSONObject json, long objId) {

        if (!(typeInfo instanceof FeedRenderer)) {
            return;
        }

        String feedName = feedUri.getLastPathSegment();
        long timestamp = new Date().getTime();

        Uri visibleFeed = App.instance().getCurrentFeed();
        String unread = "0";
        if (!feedUri.equals(visibleFeed)) {
            unread = Group.NUM_UNREAD + " + 1";
        }

        getDBHelper().getWritableDatabase().execSQL(
                "UPDATE " + Group.TABLE +
                " SET " + Group.NUM_UNREAD + " = " + unread +
                " , " + Group.LAST_OBJECT_ID + " = " + objId +
                " , " + Group.LAST_UPDATED + " = " + String.valueOf(timestamp) +
                " WHERE " + Group.FEED_NAME + " = '" + feedName + "'");
        Uri feedlistUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
        context.getContentResolver().notifyChange(feedlistUri, null);
    }
}
