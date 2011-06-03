package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Displays a list of all user-accessible threads (feeds).
 *
 */
public class FeedListActivity extends ListActivity {
    private static final String TAG = "DungBeetle";
    private FeedListCursorAdapter mFeeds;
    private ContactCache mContactCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContactCache = new ContactCache(this);
        setContentView(R.layout.feeds);
        Uri feedlist = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
        Cursor c = getContentResolver().query(feedlist, null, getFeedObjectClause(), null, null);
        mFeeds = new FeedListCursorAdapter(this, c);
        setListAdapter(mFeeds);
    }

    @Override
    public void finish() {
        super.finish();
        mContactCache.close();
    }

    private class FeedListCursorAdapter extends CursorAdapter {
        public FeedListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.feed_entry, parent, false);
            bindView(v, context, c);
            return v;
        }

        @Override
        public void bindView(final View v, final Context context, final Cursor c) {
            String[] cols = c.getColumnNames();
            int feedCol = -1;
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].equals(DbObject.FEED_NAME)) {
                    feedCol = i;
                    break;
                }
            }
            
            String feedName = c.getString(feedCol);
            String groupName = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            
            TextView labelView = (TextView) v.findViewById(R.id.feed_label);
            DbObject.bindView(v, FeedListActivity.this, c, mContactCache);
            v.setTag(R.id.feed_label, feedName);
            if (groupName != null) {
                Group g = new Group(c);
                v.setTag(R.id.group_name, g.name);
                v.setTag(R.id.group_id, g.id);
                v.setTag(R.id.group_uri, g.dynUpdateUri);
                labelView.setText(g.name);
            } else {
                labelView.setText(feedName);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String feedId = (String)v.getTag(R.id.feed_label);
        String groupName = (String)v.getTag(R.id.group_name);

        Intent launch = new Intent();
        if (groupName != null) {
            launch.setClass(FeedListActivity.this, GroupsTabActivity.class);
            launch.putExtra("group_name", groupName);
            launch.putExtra("group_id", (Long)v.getTag(R.id.group_id));
            launch.putExtra("group_uri", (String)v.getTag(R.id.group_uri));
        } else {
            launch.setClass(FeedListActivity.this, ObjectsActivity.class);
            launch.putExtra("feed_id", feedId);
        }

        startActivity(launch);
    }

    private String getFeedObjectClause() {
        // TODO: Enumerate all Object classes, look for FeedRenderables.

        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }
}
