package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.database.Cursor;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public abstract class DatabaseObjHandler extends ObjHandler {
    private final DBHelper mHelper;
    
    public DatabaseObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    public final long getObjectId(Uri feedUri, long contactID, long sequenceID) {
        String feedName = feedUri.getLastPathSegment();
        long objId = -1;
        String table = DbObject.TABLE;
        String[] projection = new String[] { DbObject._ID };
        String selection = DbObject.FEED_NAME + "=? AND "  + DbObject.CONTACT_ID + "=? AND " + DbObject.SEQUENCE_ID + "=?";
        String[] selectionArgs = new String[] {feedName, String.valueOf(contactID), String.valueOf(sequenceID)};
        
        Cursor objC = mHelper.getReadableDatabase().query(table, projection, selection, selectionArgs, null, null, null, null);
        
        if (objC.moveToFirst()) {
            objId = objC.getLong(0);
        }
        objC.close();
        return objId;
    }

    protected DBHelper getDBHelper() {
        return mHelper;
    }

    @Override
    public final void handleObj(Uri feedUri, long contactId, long sequenceId, String type, JSONObject json) {
        long objId = getObjectId(feedUri, contactId, sequenceId);
        handleObj(feedUri, objId);
    }

    public abstract void handleObj(Uri feedUri, long objId);
}
