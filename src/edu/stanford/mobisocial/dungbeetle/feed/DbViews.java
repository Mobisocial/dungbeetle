package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.view.MapView;
import edu.stanford.mobisocial.dungbeetle.feed.view.StatView;

public class DbViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new FeedView("Stats", StatView.class));
        sFeedViews.add(new FeedView("Map", MapView.class));
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }
}
