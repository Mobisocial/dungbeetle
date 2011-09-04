package edu.stanford.mobisocial.dungbeetle.feed.presence;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;

public class Presence {
    private static final List<FeedPresence> sFeedPresence = new ArrayList<FeedPresence>();
    static {
        sFeedPresence.add(new MusicPresence());
        sFeedPresence.add(new PhotosPresence());
        sFeedPresence.add(new LocationPresence());
        sFeedPresence.add(Push2TalkPresence.getInstance());
        sFeedPresence.add(InterruptMePresence.getInstance());
        sFeedPresence.add(new PhonePresence());
    }

    public static List<FeedPresence> getActivePresenceTypes() {
        List<FeedPresence> presence = new ArrayList<FeedPresence>();
        for (FeedPresence p : sFeedPresence) {
            if (p.isActive()) {
                presence.add(p);
            }
        }
        return presence;
    }
}
