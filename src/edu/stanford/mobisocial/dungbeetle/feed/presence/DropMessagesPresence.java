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
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.handler.ObjHandler;

/**
 * Drop messages. Good for canning spam.
 *
 */
public class DropMessagesPresence extends FeedPresence {
    private boolean mDropMessages = false;
    private static DropMessagesPresence sInstance;

    // TODO: proper singleton.
    public static DropMessagesPresence getInstance() {
        if (sInstance == null) {
            sInstance = new DropMessagesPresence();
        }
        return sInstance;
    }

    private DropMessagesPresence() {}

    @Override
    public String getName() {
        return "Drop Messages";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mDropMessages) {
            if (getFeedsWithPresence().size() == 0) {
                Toast.makeText(context, "No longer ignoring your friends.", Toast.LENGTH_SHORT).show();
                mDropMessages = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                Toast.makeText(context, "Now ignoring your friends.", Toast.LENGTH_SHORT).show();
                mDropMessages = true;
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
            while (mDropMessages) {
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {}

                if (!mDropMessages) {
                    break;
                }

                Helpers.sendToFeeds(mContext, obj, getFeedsWithPresence());
            }
        }
    }

    public static class MessageDropHandler extends ObjHandler {

        public boolean preFiltersObj(Context context, Uri feedUri) {
            return getInstance().getFeedsWithPresence().contains(feedUri);
        }

        @Override
        public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj) {
        }
    }
}
