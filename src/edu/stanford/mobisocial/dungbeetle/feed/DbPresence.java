package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;

public class DbPresence {

    // TODO: Use reflection.
    private static final List<FeedPresence> sFeedPresence = new ArrayList<FeedPresence>();
    static {
        //sFeedPresence.add(new MusicAction());
    }
}
