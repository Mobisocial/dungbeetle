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

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.MusicObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * Broadcast music playback events to feeds.
 *
 */
public class MusicPresence extends FeedPresence {
    private boolean mShareMusic = false;

    @Override
    public String getName() {
        return "Music";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mShareMusic) {
            if (getFeedsWithPresence().size() == 0) {
                context.getApplicationContext().unregisterReceiver(mReceiver);
                Toast.makeText(context, "No longer sharing music", Toast.LENGTH_SHORT).show();
                mShareMusic = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                IntentFilter iF = new IntentFilter();
                iF.addAction("com.android.music.metachanged");
                context.getApplicationContext().registerReceiver(mReceiver, iF);
                Toast.makeText(context, "Now sharing music", Toast.LENGTH_SHORT).show();
                mShareMusic = true;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mShareMusic) {
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                DbObject obj = MusicObj.from(artist, album, track);
                try {
                    if (intent.hasExtra("url")) {
                        obj.getJson().put(MusicObj.URL, intent.getStringExtra("url"));
                        if (intent.hasExtra("mimeType")) {
                            obj.getJson().put(MusicObj.MIME_TYPE,
                                    intent.getStringExtra("mimeType"));
                        }
                    }
                } catch (JSONException e) {}
                for (Uri feedUri : getFeedsWithPresence()) {
                    Helpers.sendToFeed(context.getApplicationContext(), obj, feedUri);
                }
            }
        }
    };
}
