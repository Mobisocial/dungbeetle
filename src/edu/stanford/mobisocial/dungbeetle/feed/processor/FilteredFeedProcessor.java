package edu.stanford.mobisocial.dungbeetle.feed.processor;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.activity.ViewActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedProcessor;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

public class FilteredFeedProcessor extends FeedProcessor {
    private final ContactCache mContactCache;
    private final Set<String> mForbidden = new HashSet<String>();

    public FilteredFeedProcessor(ContactCache contactCache) {
        mContactCache = contactCache;
        mForbidden.add(LocationObj.TYPE);
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public ListAdapter getListAdapter(Context context, Uri feedUri) {
        Cursor c = context.getContentResolver().query(feedUri, null, getFeedObjectClause(),
                null, DbObject._ID + " DESC");
        return new ObjectListCursorAdapter(context, c);
    }

    private class ObjectListCursorAdapter extends CursorAdapter {
        public ObjectListCursorAdapter (Context context, Cursor c) {
            super(context, c);
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
            DbObject.bindView(v, context, c, mContactCache);
        }
    }

    private String getFeedObjectClause() {
        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            if (!filterType(type)) {
                allowed.append(",'").append(type).append("'");
            }
        }
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }

    private boolean filterType(String type) {
        return mForbidden.contains(type);
    }
}
