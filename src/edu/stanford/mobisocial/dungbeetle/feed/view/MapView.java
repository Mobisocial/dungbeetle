package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.support.v4.app.Fragment;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMapFragment;

public class MapView implements FeedView {
    @Override
    public String getName() {
        return "Map";
    }

    @Override
    public Fragment getFragment() {
        return new FeedMapFragment();
    }
}
