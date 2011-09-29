package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

public class FeedSlideshowFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";

	public static final String TAG = "ObjectsActivity";
	private ContentObserver mFeedObserver;
	private ContactCache mContactCache;
	private Uri mFeedUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View tabs = getActivity().findViewById(R.id.tab_frame);
		if (tabs != null) {
		    tabs.setVisibility(View.GONE);
		}
		View view = inflater.inflate(R.layout.objects_item, container, false);
		view.setLayoutParams(CommonLayouts.FULL_WIDTH);
		view.setId(R.id.feed_view);
		return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContactCache = new ContactCache(getActivity());
        mFeedObserver = new ContentObserver(new Handler(getActivity().getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                // TODO, queue to end of list if we don't requery
            }
        };
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
        ContentResolver resolver = getActivity().getContentResolver();
        resolver.registerContentObserver(mFeedUri, true, mFeedObserver);
        startSlideshow();   
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getContentResolver().unregisterContentObserver(mFeedObserver);
    }

    public static String getFeedObjectClause() {
        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }

    private void startSlideshow() {
        Cursor c = getActivity().getContentResolver().query(mFeedUri, null, getFeedObjectClause(),
                null, DbObject._ID + " ASC");
        if (!c.moveToFirst()) {
            Log.d(TAG, "No items to display.");
            return;
        }

        while (!c.isLast()) {
            View v = getActivity().findViewById(R.id.feed_view);
            DbObject.bindView(v, getActivity(), c, mContactCache, false);

            // TODO: background thread that wakes up and updates foreground activity.
            try {
                Thread.sleep(3000); // TODO: diff timestamps; beware of sync death!
            } catch (InterruptedException e) { }
        }
    }
}