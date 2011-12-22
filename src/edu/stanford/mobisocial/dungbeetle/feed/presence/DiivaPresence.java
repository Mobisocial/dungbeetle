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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LinkObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class DiivaPresence extends FeedPresence {
    private boolean mShareDiiva = false;

    @Override
    public String getName() {
        return "DiiVA";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mShareDiiva) {
            if (getFeedsWithPresence().size() == 0) {
                context.getApplicationContext().unregisterReceiver(mReceiver);
                Toast.makeText(context, "No longer sharing DiiVA", Toast.LENGTH_SHORT).show();
                mShareDiiva = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                IntentFilter iF = new IntentFilter();
                iF.addAction("edu.stanford.mobisocial.Musubi.DiiVA");
                context.getApplicationContext().registerReceiver(mReceiver, iF);
                Toast.makeText(context, "Now sharing DiiVA", Toast.LENGTH_SHORT).show();
                mShareDiiva = true;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mShareDiiva) {
            	
                String url = intent.getStringExtra("url");
                String mimetype = intent.getType();
                String title = intent.getStringExtra("title");
                DbObject obj = LinkObj.from(url, mimetype, title);
                
                for (Uri feedUri : getFeedsWithPresence()) {
                    Helpers.sendToFeed(context.getApplicationContext(), obj, feedUri);
                }
            }
        }
    };
}
