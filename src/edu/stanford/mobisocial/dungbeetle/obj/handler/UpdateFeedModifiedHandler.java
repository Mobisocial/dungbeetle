package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Date;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.model.Group;

/**
 * Updates the database's "last modified" fields.
 *
 */
public class UpdateFeedModifiedHandler extends DatabaseObjHandler {
    private final String TAG = "feedmod";

    public UpdateFeedModifiedHandler(DBHelper helper) {
        super(helper);
    }

    @Override
    public void handleObj(Uri feedUri, long objId) {
        String feedName = feedUri.getLastPathSegment();
        Log.i(TAG, "objectID=" + objId + " feedName=" + feedName);
        long timestamp = new Date().getTime();
        
        ContentValues cv = new ContentValues();
        cv.put(Group.LAST_UPDATED, String.valueOf(timestamp));
        cv.put(Group.LAST_OBJECT_ID, objId);
        getDBHelper().getWritableDatabase().update(Group.TABLE, cv, Group.FEED_NAME + "=?", new String[]{feedName});
    }
}
