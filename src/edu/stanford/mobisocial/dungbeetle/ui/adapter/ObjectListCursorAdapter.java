package edu.stanford.mobisocial.dungbeetle.ui.adapter;

import java.util.Date;

import android.content.ContentValues;
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
        String feedName = c.getString(1);
        
//        ContentValues cv = new ContentValues();
//        cv.put(Group.NUM_UNREAD, 0);
//        //TODO we need to cache this right? no reason to write to db every row
//        context.getContentResolver().update(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Group.TABLE), cv, Group.FEED_NAME+"='"+feedName+"'", null);
//        
//        context.getContentResolver().notifyChange(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist"), null);


    }

    public static CursorLoader queryObjects(Context context, Uri feedUri) {
        return new CursorLoader(context, feedUri, 
        	new String[] { 
        		DbObject._ID,
        		DbObject.FEED_NAME
        	},
        	DbObjects.getFeedObjectClause(), null, DbObject._ID + 
//        	" DESC LIMIT 15");
    		" DESC");
    }
    public CursorLoader queryLaterObjects(Context context, Uri feedUri, int total) {
    	int newTotal = total + 15;
    	CursorLoader cl = new CursorLoader(context, feedUri, 
            	new String[] { 
            		DbObject._ID,
            		DbObject.FEED_NAME
            	},
            	DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC LIMIT " + newTotal);
		changeCursor(cl.loadInBackground());
		return cl;
    }
}
