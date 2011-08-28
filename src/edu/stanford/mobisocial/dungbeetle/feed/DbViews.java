package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.view.HeadView;

public class DbViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new HeadView());
        //sFeedViews.add(new MapView());
        //sFeedViews.add(new StatsView());
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }
}
