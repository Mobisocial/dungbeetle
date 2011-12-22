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

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * Sends messages rapidly. For testing, not annoying friends!
 *
 */
public class SpamPresence extends FeedPresence {
    private boolean mShareSpam = false;
    private SpamThread mSpamThread;

    @Override
    public String getName() {
        return "Spam";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mShareSpam) {
            if (getFeedsWithPresence().size() == 0) {
                Toast.makeText(context, "No longer spamming", Toast.LENGTH_SHORT).show();
                mShareSpam = false;
                mSpamThread = null;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                Toast.makeText(context, "Now spamming your friends", Toast.LENGTH_SHORT).show();
                mShareSpam = true;
                mSpamThread = new SpamThread(context);
                mSpamThread.start();
            }
        }
    }

    class SpamThread extends Thread {
        long WAIT_TIME = 500;
        final Context mContext;

        public SpamThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            DbObject obj = StatusObj.from("StatusObj spam, StatusObj spam.");
            while (mShareSpam) {
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {}

                if (!mShareSpam) {
                    break;
                }

                Helpers.sendToFeeds(mContext, obj, getFeedsWithPresence());
            }
        }
    }
}
