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

package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.CameraAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.GalleryAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.LaunchApplicationAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.PresenceAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.VoiceAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;

public class DbActions {

    // TODO: Use reflection.
    private static final List<FeedAction> sFeedActions = new ArrayList<FeedAction>();
    static {
        sFeedActions.add(new PresenceAction());
        sFeedActions.add(new CameraAction());
        sFeedActions.add(new GalleryAction());
        sFeedActions.add(new VoiceAction());
        sFeedActions.add(new LaunchApplicationAction());
    }

    public static final QuickAction getActions(final Context c, final Uri feedUri, final View v) {
        final QuickAction qa = new QuickAction(v);
        for (final FeedAction action : sFeedActions) {
            if (!action.isActive()) continue;

            final ActionItem item = new ActionItem();
            item.setTitle(action.getName());
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    qa.dismiss();
                    action.onClick(c, feedUri);
                }
            });
            qa.addActionItem(item);
        }
        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
        return qa;
    }
}
