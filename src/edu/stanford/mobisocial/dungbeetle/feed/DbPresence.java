package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.LocationPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.MusicPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.PhotosPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.InterruptMePresence;

public class DbPresence {
    private static final List<FeedPresence> sFeedPresence = new ArrayList<FeedPresence>();
    static {
        sFeedPresence.add(new MusicPresence());
        sFeedPresence.add(new PhotosPresence());
        sFeedPresence.add(new LocationPresence());
        sFeedPresence.add(InterruptMePresence.getInstance());
        //sFeedPresence.add(new PhonePresence());
    }

    public static List<FeedPresence> getPresenceTypes() {
        return sFeedPresence;
    }
}
