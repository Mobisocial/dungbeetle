/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.feed.presence;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class Presence {
    private static final List<FeedPresence> sFeedPresence = new ArrayList<FeedPresence>();
    static {
        sFeedPresence.add(new PhotosPresence());
    	if (MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
    	    sFeedPresence.add(new VideosPresence());
            sFeedPresence.add(new MusicPresence());
            sFeedPresence.add(new DiivaPresence());
            sFeedPresence.add(new LocationPresence());
            sFeedPresence.add(new PhonePresence());
            sFeedPresence.add(Push2TalkPresence.getInstance());
            sFeedPresence.add(TVModePresence.getInstance());
            sFeedPresence.add(new SpamPresence());
            sFeedPresence.add(DropMessagesPresence.getInstance());
    	}
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
