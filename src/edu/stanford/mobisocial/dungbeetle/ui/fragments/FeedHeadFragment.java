package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

public class FeedHeadFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";
    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

	public static final String TAG = "ObjectsActivity";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.objects_item, container, false);
		view.setLayoutParams(LAYOUT_FULL_WIDTH);
		view.setId(R.id.feed_view);
		return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri feedUri = getArguments().getParcelable(ARG_FEED_URI);
        Cursor c = getActivity().getContentResolver().query(feedUri, null, getFeedObjectClause(),
                null, DbObject._ID + " DESC");
        if (c.moveToFirst()) {
            View v = getActivity().findViewById(R.id.feed_view);
            // TODO: move to activity, pulled via interface
            ContactCache cache = new ContactCache(getActivity());
            DbObject.bindView(v, getActivity(), c, cache);
        }
    }

    public static String getFeedObjectClause() {
        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }
}