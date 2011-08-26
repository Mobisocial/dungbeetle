package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.FeedMapActivity;
import edu.stanford.mobisocial.dungbeetle.ui.FeedStatsActivity;

public class DbViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new FeedView("Stats", FeedStatsActivity.class));
        sFeedViews.add(new FeedView("Map", FeedMapActivity.class));
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }
}
