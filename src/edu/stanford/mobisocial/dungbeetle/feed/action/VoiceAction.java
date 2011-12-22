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

package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.VoiceQuickRecordActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

/**
 * Record a voice note to share with a feed.
 *
 */
public class VoiceAction implements FeedAction { // TODO: Move to VoiceObj implements FeedAction

    @Override
    public String getName() {
        return "Voice";
    }

    @Override
    public void onClick(Context context, Uri feedUri) {
        Intent record = new Intent();
        record.setClass(context, VoiceQuickRecordActivity.class);
        record.putExtra(FeedViewFragment.ARG_FEED_URI, feedUri);
        context.startActivity(record);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
