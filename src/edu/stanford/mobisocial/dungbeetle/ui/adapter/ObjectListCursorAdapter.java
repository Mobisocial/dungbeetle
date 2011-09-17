package edu.stanford.mobisocial.dungbeetle.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

public class ObjectListCursorAdapter extends CursorAdapter {
    private ContactCache mContactCache;

    public ObjectListCursorAdapter (Context context, Cursor cursor) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        mContactCache = new ContactCache(context); // TODO: Global contact cache
        // TODO: does contact cache handle images and attributes?
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.objects_item, parent, false);
        bindView(v, context, c);
        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {
        DbObject.bindView(v, context, c, mContactCache, true);
        DBHelper helper = new DBHelper(context);
        int feedCol = -1;
        String[] cols = c.getColumnNames();
        // There are two selected 'feed_name' columns, one can be null.
        for (int i = 0; i < cols.length; i++) {
            if (cols[i].equals(DbObject.FEED_NAME)) {
                feedCol = i;
                break;
            }
        }

        String feedName = c.getString(feedCol);
        helper.markFeedAsRead(feedName);
        helper.close();
        context.getContentResolver().notifyChange(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist"), null);


    }

    public static CursorLoader queryObjects(Context context, Uri feedUri) {
        return new CursorLoader(context, feedUri, null,
                DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC LIMIT 0, 15");
    }
    
    public void queryLaterObjects(Context context, Uri feedUri, int total) {
    	total += 15;
    	Cursor newCursor = (new CursorLoader(context, feedUri, null,DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC LIMIT " + total)).loadInBackground(); 
        this.swapCursor(newCursor);
    }
}
