package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * TODO: The goal is for DatabaseObjHandler to accept objects from
 * a source (network or local user) and insert them in the database.
 * 
 * Once inserted, any interested object can act on the data via DbInsertionHandler
 *
 */
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
        
        Cursor objC = mHelper.getReadableDatabase().query(table, projection, selection, selectionArgs, null, null, DbObject.CONTACT_ID + " DESC", "1");
        
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
    public final void handleObj(Context context, Uri feedUri, Contact contact,
            long sequenceId, DbEntryHandler typeInfo, JSONObject json) {
        long objId = getObjectId(feedUri, contact.id, sequenceId);
        handleObj(context, feedUri, objId);
    }

    public abstract void handleObj(Context context, Uri feedUri, long objId);
}
