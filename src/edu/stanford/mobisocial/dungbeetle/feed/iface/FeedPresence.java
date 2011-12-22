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

package edu.stanford.mobisocial.dungbeetle.feed.iface;

import java.util.LinkedHashSet;

import android.content.Context;
import android.net.Uri;

/**
 * Base class for long-lasting activities associated with a feed.
 *
 */
public abstract class FeedPresence {
    public final LinkedHashSet<Uri> mActiveFeeds = new LinkedHashSet<Uri>();
    public abstract String getName();
    protected static String TAG = "feedPresence";
    protected final static boolean DBG = true;

    public final void setFeedPresence(Context context, Uri feed, boolean present) {
        if (present) {
            mActiveFeeds.add(feed);
        } else {
            mActiveFeeds.remove(feed);
        }
        onPresenceUpdated(context, feed, present);
    }

    protected abstract void onPresenceUpdated(Context context, Uri feed, boolean present);

    public final LinkedHashSet<Uri> getFeedsWithPresence() {
        return mActiveFeeds;
    }

    public boolean isPresent(Uri feedUri) {
        return mActiveFeeds.contains(feedUri);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isActive() {
        return true;
    }
}
