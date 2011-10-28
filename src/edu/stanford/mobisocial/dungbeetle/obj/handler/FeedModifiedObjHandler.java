package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Date;

import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.net.Uri;
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
    final DBHelper mHelper;

    public FeedModifiedObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    @Override
    public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        long objId = obj.getLocalId();
        if (!(typeInfo instanceof FeedRenderer)) {
            return;
        }

        switch(Feed.typeOf(feedUri)) {
	        case Feed.FEED_FRIEND: {
	            long contact_id = Long.valueOf(feedUri.getLastPathSegment());
	            long timestamp = new Date().getTime();

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
	                    " WHERE " + Contact._ID + " = '" + contact_id + "'");
	            Uri contactsUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
	            context.getContentResolver().notifyChange(contactsUri, null);
	        	break;
	        }
	        case Feed.FEED_RELATED: {
	        	throw new RuntimeException("you should never be getting a message in on a related feed, its a virtual feed");
	        }
	        case Feed.FEED_GROUP: {
	            String feedName = feedUri.getLastPathSegment();
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
	            Uri feedlistUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
	            context.getContentResolver().notifyChange(feedlistUri, null);
	        	break;
	        }
    	}
    }
}
