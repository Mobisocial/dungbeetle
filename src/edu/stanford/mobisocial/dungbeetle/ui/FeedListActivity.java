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

package edu.stanford.mobisocial.dungbeetle.ui;

import mobisocial.socialkit.musubi.DbUser;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedActionsFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

/**
 * Displays a list of all user-accessible threads (feeds).
 */
public class FeedListActivity extends MusubiBaseActivity implements
        FeedListFragment.OnFeedSelectedListener {

    private static final String FRAGMENT_FEED_ACTIONS = "feedActions";
    private boolean mDualPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_list);
        MusubiBaseActivity.doTitleBar(this);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.feed_list, new FeedListFragment()).commit();
        mDualPane = (null != findViewById(R.id.feed_view));
    }

    @Override
    public void onFeedSelected(Uri feedUri) {
        if (mDualPane) {
            Bundle args = new Bundle();
            args.putParcelable(FeedViewFragment.ARG_FEED_URI, feedUri);
            args.putBoolean(FeedViewFragment.ARG_DUAL_PANE, mDualPane);
            Fragment feedView = new FeedViewFragment();
            Fragment feedActions = new FeedActionsFragment();
            feedView.setArguments(args);
            feedActions.setArguments(args);
            Fragment oldSelector =
                    getSupportFragmentManager().findFragmentByTag(FRAGMENT_FEED_ACTIONS);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (oldSelector != null) {
                ft.remove(oldSelector);
            }
            ft.add(feedActions, FRAGMENT_FEED_ACTIONS);
            ft.replace(R.id.feed_view, feedView);
            ft.commit();
        } else {
            if (Feed.typeOf(feedUri) == Feed.FeedType.FRIEND) {
                String personId = Feed.friendIdForFeed(feedUri);
                DbUser u = App.instance().getMusubi().userForGlobalId(feedUri, personId);
                Helpers.getContact(this, u.getLocalId()).view(this);
            } else {
                Intent launch = new Intent(Intent.ACTION_VIEW);
                launch.setDataAndType(feedUri, Feed.MIME_TYPE);
                startActivity(launch);
            }
        }
    }
}
