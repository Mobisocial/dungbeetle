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
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class FeedHeadFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";
    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

	public static final String TAG = "ObjectsActivity";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = new FrameLayout(getActivity());
		view.setLayoutParams(LAYOUT_FULL_WIDTH);
		return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri feedUri = getArguments().getParcelable(ARG_FEED_URI);
        Cursor c = getActivity().getContentResolver().query(feedUri, null, getFeedObjectClause(),
                null, DbObject._ID + " DESC");
        if (c.moveToFirst()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
            Toast.makeText(getActivity(), jsonSrc, 500).show();   
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