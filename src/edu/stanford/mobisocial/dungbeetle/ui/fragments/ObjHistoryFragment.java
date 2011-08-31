package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

public class ObjHistoryFragment extends Fragment {
    private static final String TAG = "dbobjPager";

    private Uri mFeedUri;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = activity.getIntent().getData();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Gallery gallery = new Gallery(getActivity());
        gallery.setLayoutParams(CommonLayouts.FULL_WIDTH);
        return gallery;
    }
}
