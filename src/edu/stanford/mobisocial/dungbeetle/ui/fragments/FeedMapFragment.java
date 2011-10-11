package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class FeedMapFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";
    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

	public static final String TAG = "ObjectsActivity";
	private ContentObserver mFeedObserver;
	private Uri mFeedUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ContentResolver resolver = getActivity().getContentResolver();
        resolver.registerContentObserver(mFeedUri, true, mFeedObserver);

		View view = new WebView(getActivity());
		view.setLayoutParams(LAYOUT_FULL_WIDTH);
		view.setId(android.R.id.custom);
		return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedObserver = new ContentObserver(new Handler(activity.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                onFeedUpdated();
            }
        };
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onFeedUpdated();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getContentResolver().unregisterContentObserver(mFeedObserver);
    }

    private void onFeedUpdated() {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("http://maps.googleapis.com/maps/api/staticmap?size=512x512&sensor=true&path=");
        pathBuilder.append("color:0x0000ff|weight:5");

        Cursor c = getActivity().getContentResolver().query(mFeedUri, null, getLocationClause(),
                null, DbObject._ID + " DESC");
        try {
	        int totalSize = c.getCount();
	        c.moveToFirst();
	        // TODO: This graphs all points in a path.
	        // You probably want unique paths for each user.
	        for (int i = 1; i < totalSize; i++) {
	            c.moveToNext();
	            DbObject obj = DbObject.fromCursor(c);
	            String lat = obj.getJson().optString(LocationObj.COORD_LAT);
	            String lon = obj.getJson().optString(LocationObj.COORD_LONG);
	            pathBuilder.append("|").append(lat).append(",").append(lon);
	        }
        } finally {
        	c.close();
        }
        Uri data = Uri.parse(pathBuilder.toString());
        ((WebView)getActivity().findViewById(android.R.id.custom)).loadUrl(data.toString());
    }

    private String getLocationClause() {
        return DbObject.TYPE + " = '" + LocationObj.TYPE + "'";
    }    
}