package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

/**
 * Displays a list of all user-accessible threads (feeds).
 *
 */
public class FeedListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "DungBeetle";
    private FeedListCursorAdapter mFeeds;
    private ContactCache mContactCache;
    private DBHelper mHelper;
    private OnFeedSelectedListener mFeedSelectedListener;
    
    public interface OnFeedSelectedListener {
        public void onFeedSelected(Uri feedUri);
    }
   
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mFeedSelectedListener = (OnFeedSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement OnFeedSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (null != getActivity().findViewById(R.id.feed_view)) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        mHelper = new DBHelper(getActivity());
        mContactCache = new ContactCache(getActivity());
        getLoaderManager().initLoader(0, null, this);
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
            // There are two selected 'feed_name' columns, one can be null.
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].equals(DbObject.FEED_NAME)) {
                    feedCol = i;
                    break;
                }
            }

            String feedName = c.getString(feedCol);
            String groupName = c.getString(c.getColumnIndexOrThrow(Group.NAME));

            TextView labelView = (TextView) v.findViewById(R.id.feed_label);
            DbObject.bindView(v, getActivity(), c, mContactCache, false);
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
        getListView().setItemChecked(position, true);
        String feedName = (String)v.getTag(R.id.feed_label);
        while (feedName == null) {
            v = (View)v.getParent();
            if (v == null) {
                Log.w(TAG, "No feed information found.");
                break;
            }
            feedName = (String)v.getTag(R.id.feed_label);
        }
        Uri feedUri = Feed.uriForName(feedName);
        mFeedSelectedListener.onFeedSelected(feedUri);
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

    @SuppressWarnings("unused")
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

    private Loader<Cursor> mLoader;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri feedlist = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
        if (mLoader == null) {
            mLoader = new CursorLoader(getActivity(), feedlist, null, getFeedObjectClause(), null, null);
        }
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mFeeds = new FeedListCursorAdapter(getActivity(), cursor);
        setListAdapter(mFeeds);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }
}
