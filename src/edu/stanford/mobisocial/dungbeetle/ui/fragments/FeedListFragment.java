package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.GroupsTabActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.FeedViewActivity;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

/**
 * Displays a list of all user-accessible threads (feeds).
 *
 */
public class FeedListFragment extends ListFragment {
    private static final String TAG = "DungBeetle";
    private FeedListCursorAdapter mFeeds;
    private ContactCache mContactCache;
    private DBHelper mHelper;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContactCache = new ContactCache(getActivity());
        Uri feedlist = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
        Cursor c = getActivity().getContentResolver().query(
                feedlist, null, getFeedObjectClause(), null, null);
        mFeeds = new FeedListCursorAdapter(getActivity(), c);
        mHelper = new DBHelper(getActivity());

        setListAdapter(mFeeds);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed_list, container, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
            DbObject.bindView(v, getActivity(), c, mContactCache);
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
            int color = Feed.colorFor(feedName);
            labelView.setBackgroundColor(color);
            //v.setBackgroundColor(color);
            //v.getBackground().setAlpha(Feed.BACKGROUND_ALPHA);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String feedId = (String)v.getTag(R.id.feed_label);
        while (feedId == null) {
            v = (View)v.getParent();
            if (v == null) {
                Log.w(TAG, "No feed information found.");
                break;
            }
            feedId = (String)v.getTag(R.id.feed_label);
        }
        String groupName = null;
        Long groupId = null;
        String groupUri = null;
        if (v != null) {
            groupName = (String)v.getTag(R.id.group_name);
            groupId = (Long)v.getTag(R.id.group_id);
            groupUri = (String)v.getTag(R.id.group_uri);
        }

        Intent launch = new Intent();
        if (groupName != null) {
            launch.setClass(getActivity(), GroupsTabActivity.class);
            launch.putExtra("group_name", groupName);
            launch.putExtra("group_id", groupId);
            launch.putExtra("group_uri", groupUri);
        } else {
            launch.setClass(getActivity(), FeedViewActivity.class);
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
        return DbObject.TYPE + " in (" + allowed.substring(1) + ") AND " +
            DbObject.TABLE + "." + DbObject.FEED_NAME + " NOT IN ('direct', 'friend', '')";
    }

    private OnClickListener newFeed() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage("Enter group name:");
                final EditText input = new EditText(getActivity());
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String groupName = input.getText().toString();
                            Group g;
                            if(groupName.length() > 0) {
                                g = Group.create(getActivity(), groupName, mHelper);
                            }
                            else {
                                g = Group.create(getActivity());
                            }
                            
                            Helpers.sendToFeed(getActivity(),
                                    StatusObj.from("Welcome to " + g.name + "!"),
                                    Feed.uriForName(g.feedName));
                        }
                    });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                alert.show();
            }
        };
    }
}
