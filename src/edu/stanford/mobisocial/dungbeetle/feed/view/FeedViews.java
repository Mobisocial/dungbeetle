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

package edu.stanford.mobisocial.dungbeetle.feed.view;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMembersFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedSlideshowFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.AppsViewFragment;

public class FeedViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new DefaultView());
        sFeedViews.add(FeedViews.feedViewFrom("Members", new FeedMembersFragment()));
        sFeedViews.add(new MapView());
        sFeedViews.add(new PartyView());
        sFeedViews.add(FeedViews.feedViewFrom("Timeshow", new FeedSlideshowFragment()));
        sFeedViews.add(new PresenceView());
        //sFeedViews.add(new StatsView());
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }

    public static void promptForView(final Context context, final Uri feedUri) {
        String[] items = new String[getFeedViews().size()];
        int i = 0;
        for (FeedView v : getFeedViews()) {
            items[i++] = v.getName();
        }

        new AlertDialog.Builder(context)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FeedView v = FeedViews.getFeedViews().get(which);
                    Fragment f = v.getFragment();
                    Bundle args = new Bundle();
                    args.putParcelable("feed_uri", feedUri);
                    f.setArguments(args);
                    ((FragmentActivity)context).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.feed_view, f)
                            .addToBackStack(null).commit();
                }
            })
            .setTitle("View...")
            .create().show();
    }

    public static FeedView feedViewFrom(final String name, final Fragment fragment) {
        return new FeedView() {
            @Override
            public Fragment getFragment() {
                return fragment;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
