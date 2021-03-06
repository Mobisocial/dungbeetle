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

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IObjHandler;

/**
 * Automatically plays back audio clips as they are received.
 *
 */
public class Push2TalkPresence extends FeedPresence implements IObjHandler {
    private static final String TAG = "push2talk";
    private boolean mEnabled = false;
    private static Push2TalkPresence sInstance;

    private Push2TalkPresence() {

    }

    @Override
    public String getName() {
        return "Push2Talk";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        mEnabled = getFeedsWithPresence().size() > 0;
    }

    public static Push2TalkPresence getInstance() {
        if (sInstance == null) {
            sInstance = new Push2TalkPresence();
        }
        return sInstance;
    }

    @Override
    public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        if (!mEnabled || !getFeedsWithPresence().contains(feedUri) ||
                !(typeInfo instanceof VoiceObj)) {
            return;
        }

        if (DBG) Log.d(TAG, "Playing audio via push2talk on " + feedUri);
        ((VoiceObj) typeInfo).activate(context, null);
    }

    public boolean isOnCall() {
        return mEnabled;
    }
}
