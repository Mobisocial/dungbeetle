package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ViewGroup.LayoutParams;

public abstract class FeedBaseFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";
    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

	public static final String TAG = "ObjectsActivity";
	protected ContentObserver mFeedObserver;
	protected Uri mFeedUri;

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
        ContentResolver resolver = getActivity().getContentResolver();
        resolver.registerContentObserver(mFeedUri, true, mFeedObserver);
        onFeedUpdated();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getContentResolver().unregisterContentObserver(mFeedObserver);
    }

    public abstract void onFeedUpdated(); 
}