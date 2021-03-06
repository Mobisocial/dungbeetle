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

import java.util.ArrayList;
import java.util.List;

import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Filterable;
import edu.stanford.mobisocial.dungbeetle.feed.view.FeedViews;
import edu.stanford.mobisocial.dungbeetle.feed.view.PresenceView;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.AppsViewFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedActionsFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMembersFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

/**
 * Represents a group by showing its feed and members.
 */
public class FeedHomeActivity extends MusubiBaseActivity
        implements ViewPager.OnPageChangeListener, FeedListFragment.OnFeedSelectedListener, Filterable {
    private Nfc mNfc;
    private String mGroupName;
    private FeedActionsFragment mActionsFragment;
    private Uri mFeedUri;
    private int mColor;
    private ViewPager mFeedViewPager;
    private final List<Button> mButtons = new ArrayList<Button>();

    private List<FeedView> mFeedViews = new ArrayList<FeedView>();

    public final String TAG = "GroupsTabActivity";
    

    private final String[] filterTypes = DbObjects.getRenderableTypes();
    private boolean[] checked;

    public void onClickBroadcast(View v) {
        mActionsFragment.promptForSharing();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_home);
        mNfc = new Nfc(this);
        
        checked = new boolean[filterTypes.length];
    	
        for(int x = 0; x < filterTypes.length; x++) {
        	checked[x] = true;
        }

        mFeedViews = new ArrayList<FeedView>();
        mFeedViews.add(FeedViews.feedViewFrom("Feed", new FeedViewFragment()));
        mFeedViews.add(FeedViews.feedViewFrom("Apps", new AppsViewFragment()));
        mFeedViews.add(FeedViews.feedViewFrom("People", new FeedMembersFragment()));
        mFeedViews.add(new PresenceView());

        Intent intent = getIntent();
        String feed_name = null;
        String dyn_feed_uri = null;
        if (intent.getType() != null && intent.getType().equals(Feed.MIME_TYPE)) {
            Uri feedUri = getIntent().getData();
            feed_name = feedUri.getLastPathSegment();
            Maybe<Group> maybeG = Group.forFeedName(FeedHomeActivity.this, feed_name);
            try {
                Group g = maybeG.get();
                mGroupName = g.name;
                dyn_feed_uri = g.dynUpdateUri;
            } catch (Exception e) {}
        }

        if (dyn_feed_uri != null) {
            mNfc.share(NdefFactory.fromUri(dyn_feed_uri));
            Log.w(TAG, dyn_feed_uri);
        }

        mFeedUri = Feed.uriForName(feed_name);
        mColor= Feed.colorFor(feed_name);
        
        Bundle args = new Bundle();
        args.putParcelable(FeedViewFragment.ARG_FEED_URI, mFeedUri);
        mActionsFragment = new FeedActionsFragment();
        mActionsFragment.setArguments(args);

        for (FeedView f : mFeedViews) {
            f.getFragment().setArguments(args);
        }

        // TODO: Why is FeedActionsFragment.getActivity() null without this hack?
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment actions = getSupportFragmentManager().findFragmentByTag("feedActions");
        if (actions != null) {
            ft.remove(actions);
        }
        ft.add(mActionsFragment, "feedActions");
        ft.commit();

        PagerAdapter adapter = new FeedFragmentAdapter(getSupportFragmentManager(), mFeedUri);
        mFeedViewPager = (ViewPager)findViewById(R.id.feed_pager);
        mFeedViewPager.setAdapter(adapter);
        mFeedViewPager.setOnPageChangeListener(this);
        mFeedViewPager.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        ViewGroup group = (ViewGroup)findViewById(R.id.tab_frame);
        int i = 0;
        for (FeedView f : mFeedViews) {
            Button button = new Button(this);
            button.setText(f.getName());
            button.setTextSize(18f);
            
            button.setLayoutParams(CommonLayouts.FULL_HEIGHT);
            button.setTag(i++);
            button.setOnClickListener(mViewSelected);

            group.addView(button);
            mButtons.add(button);
        }

        doTitleBar(this, mGroupName);
        onPageSelected(0);
    }

    // TODO: Move to DashboardActivity, but add a FLAG_CLEAR_ON_PAUSE to EasyNfc.
    @Override
    protected void onResume() {
        super.onResume();
        mNfc.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) return;
    }

    @Override
    public void onFeedSelected(Uri feedUri) {
        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setDataAndType(feedUri, Feed.MIME_TYPE);
        startActivity(launch);
    }


    private View.OnClickListener mViewSelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Integer i = (Integer)v.getTag();
            mFeedViewPager.setCurrentItem(i);
        }
    };

    public class FeedFragmentAdapter extends FragmentPagerAdapter {
        final int NUM_ITEMS;
        final List<Fragment> mFragments = new ArrayList<Fragment>();

        public FeedFragmentAdapter(FragmentManager fm, Uri feedUri) {
            super(fm);

            NUM_ITEMS = mFeedViews.size();
            for (FeedView f : mFeedViews) {
                mFragments.add(f.getFragment());
            }
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }
    }
    

    @Override
    public void onPageScrollStateChanged(int state) {
        
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        
    }

    @Override
    public void onPageSelected(int position) {
        int c = mButtons.size();
        for (int i = 0; i < c; i++) {
            mButtons.get(i).setBackgroundColor(Color.TRANSPARENT);
        }
        mButtons.get(position).setBackgroundColor(mColor);
    }

	@Override
	public String[] getFilterTypes() {
		return filterTypes;
	}

	@Override
	public boolean[] getFilterCheckboxes() {
		return checked;
	}

	@Override
	public void setFilterCheckbox(int position, boolean check) {
		checked[position] = check;
	}
}
