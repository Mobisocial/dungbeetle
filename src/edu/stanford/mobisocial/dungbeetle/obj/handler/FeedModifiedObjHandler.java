package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Date;

import mobisocial.socialkit.musubi.DbObj;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;

/**
 * Updates the database's "last modified" fields.
 *
 */
public class FeedModifiedObjHandler extends ObjHandler {
    private static final String TAG = "feedModifiedHandler";
    final DBHelper mHelper;

    public FeedModifiedObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    @Override
    public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        String feedName = feedUri.getLastPathSegment();

        long objId = obj.getLocalId();
        if (!(typeInfo instanceof FeedRenderer)) {
            return;
        }

        switch(Feed.typeOf(feedUri)) {
	        case FRIEND: {
	            String personId = Feed.friendIdForFeed(feedUri);
	            if (personId == null) {
	                Log.w(TAG, "No contact found for feed uri " + feedUri);
	                return;
	            }
	            long timestamp = new Date().getTime();


                // Update contact unread count. TODO: remove in favor of 'friend' group?
	            Uri visibleFeed = App.instance().getCurrentFeed();
	            String unread = "0";
	            if (!feedUri.equals(visibleFeed)) {
	                unread = Contact.NUM_UNREAD + " + 1";
	            }
	            mHelper.getWritableDatabase().execSQL(
	                    "UPDATE " + Contact.TABLE +
	                    " SET " + Contact.NUM_UNREAD + " = " + unread +
	                    " , " + Contact.LAST_OBJECT_ID + " = " + objId +
	                    " , " + Contact.LAST_UPDATED + " = " + String.valueOf(timestamp) +
	                    " WHERE " + Contact.PERSON_ID + " = '" + personId + "'");
	            Uri contactsUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
	            context.getContentResolver().notifyChange(contactsUri, null);

	            // One-on-one group feed:
	            String table = Group.TABLE;
	            String[] columns = new String[] { Group._ID, Group.NAME };
	            String selection = Group.FEED_NAME + " = ?";
	            String[] selectionArgs = new String[] { feedName };
	            String groupBy = null;
	            String having = null;
	            String orderBy = null;
	            Cursor c = mHelper.getReadableDatabase().query(
	                    table, columns, selection, selectionArgs, groupBy, having, orderBy);

	            // Friendly name for this feed
	            String table2 = Contact.TABLE;
                String[] columns2 = new String[] { Contact.NAME };
                String selection2 = Contact.PERSON_ID + " = ?";
                String[] selectionArgs2 = new String[] { personId };
                Cursor cursor2 = mHelper.getReadableDatabase().query(
                        table2, columns2, selection2, selectionArgs2, null, null, null);
                String friendlyName;
                if (!cursor2.moveToFirst()) {
                    friendlyName = "Unknown";
                } else {
                    friendlyName = cursor2.getString(0);
                }

	            if (!c.moveToFirst()) {
	                // First post
	                ContentValues values = new ContentValues();
	                values.put(Group.FEED_NAME, feedName);
	                values.put(Group.GROUP_TYPE, Group.TYPE_FRIEND);
	                values.put(Group.NAME, friendlyName);
	                mHelper.getWritableDatabase().insert(table, null, values);
	            } else {
	                String currentName = c.getString(1);
	                if (!friendlyName.equals(currentName)) {
	                    ContentValues values = new ContentValues();
	                    values.put(Group.NAME, friendlyName);
    	                String whereClause = Group.FEED_NAME + " = ";
    	                String[] whereArgs = new String[] { feedName };
    	                mHelper.getWritableDatabase().update(table, values, whereClause, whereArgs);
	                }
	            }

	            // No break: also update "group feed"
	        }
	        case GROUP: {
	            long timestamp = new Date().getTime();

	            Uri visibleFeed = App.instance().getCurrentFeed();
	            String unread = "0";
	            if (!feedUri.equals(visibleFeed)) {
	                unread = Group.NUM_UNREAD + " + 1";
	            }

	            mHelper.getWritableDatabase().execSQL(
	                    "UPDATE " + Group.TABLE +
	                    " SET " + Group.NUM_UNREAD + " = " + unread +
	                    " , " + Group.LAST_OBJECT_ID + " = " + objId +
	                    " , " + Group.LAST_UPDATED + " = " + String.valueOf(timestamp) +
	                    " WHERE " + Group.FEED_NAME + " = '" + feedName + "'");
	            context.getContentResolver().notifyChange(Feed.feedListUri(), null);
	        	break;
	        }
    	}
    }
}
