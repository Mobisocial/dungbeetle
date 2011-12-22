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

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Presence;

public class PresenceAction implements FeedAction {

    @Override
    public String getName() {
        return "Engage";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        List<FeedPresence> presenceTypes = Presence.getActivePresenceTypes();
        final String[] presence = new String[presenceTypes.size()];
        final boolean[] oldPresence = new boolean[presence.length];
        final boolean[] newPresence = new boolean[presence.length];
        int i = 0;
        for (FeedPresence p : presenceTypes) {
            oldPresence[i] = p.isPresent(feedUri);
            newPresence[i] = oldPresence[i];
            presence[i++] = p.getName();
        }
        new AlertDialog.Builder(context)
            .setMultiChoiceItems(presence, newPresence,
                    new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    newPresence[which] = isChecked;
                }
            })
            .setTitle("Sharing...")
            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (int i = 0; i < presence.length; i++) {
                        if (oldPresence[i] != newPresence[i]) {
                            Presence.getActivePresenceTypes().get(i).setFeedPresence(
                                    context, feedUri, newPresence[i]);
                        }
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
            .create().show();
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
