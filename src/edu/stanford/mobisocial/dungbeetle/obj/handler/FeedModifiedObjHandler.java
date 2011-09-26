package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
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
    public void handleObj(Context context, Uri feedUri, long objId) {
        String feedName = feedUri.getLastPathSegment();
        long timestamp = new Date().getTime();

        ContentValues cv = new ContentValues();
        cv.put(Group.LAST_UPDATED, String.valueOf(timestamp));
        cv.put(Group.LAST_OBJECT_ID, objId);
        getDBHelper().getWritableDatabase().update(Group.TABLE, cv, Group.FEED_NAME + "=?", new String[]{feedName});

        Uri visibleFeed = App.instance().getCurrentFeed();
        if (!feedUri.equals(visibleFeed)) {
            getDBHelper().getWritableDatabase().execSQL("UPDATE "+Group.TABLE+" SET "+Group.NUM_UNREAD+" = "+Group.NUM_UNREAD+" + 1 WHERE "+Group.FEED_NAME+"='"+feedName+"'");
            Uri feedlistUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
            context.getContentResolver().notifyChange(feedlistUri, null);
        }
    }
}
