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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.adapter.FeedAdapter;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;


public class FeedThumberActivity extends MusubiBaseActivity
    implements ViewPager.OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor> {

    private ViewPager mFeedViewPager;
    private FeedAdapter mFeeds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_home);
        // getLoaderManager().initLoader(0, null, this); // wtf!

        Uri uri = Feed.feedListUri();
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        mFeeds = new FeedAdapter(this, cursor);
        PagerAdapter adapter = new FeedFragmentAdapter(getSupportFragmentManager(), cursor);
        mFeedViewPager = (ViewPager)findViewById(R.id.feed_pager);
        mFeedViewPager.setAdapter(adapter);
        mFeedViewPager.setOnPageChangeListener(this);
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int position) {
        Uri feedUri = mFeeds.getFeedUri(position);
        // Feed.colorFor(feedUri.getLastPathSegment());  // : )
        Maybe<Group> g = Group.forFeed(this, feedUri);
        try {
            Log.d("musubi", "VIEWING FEED NAMED: " + g.get().name);
        } catch (NoValError e) {
        }
    }

    private Loader<Cursor> mLoader;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mLoader == null) {
            mLoader = new CursorLoader(this, Feed.feedListUri(), null, null, null, null);
        }
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mFeeds = new FeedAdapter(this, cursor);
        PagerAdapter adapter = new FeedFragmentAdapter(getSupportFragmentManager(), cursor);
        mFeedViewPager = (ViewPager)findViewById(R.id.feed_pager);
        mFeedViewPager.setAdapter(adapter);
        mFeedViewPager.setOnPageChangeListener(this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    public class FeedFragmentAdapter extends FragmentPagerAdapter {
        final List<Fragment> mFragments = new ArrayList<Fragment>();
        final Bundle mArgs;
        final Cursor mCursor;

        public FeedFragmentAdapter(FragmentManager fm, Cursor cursor) {
            super(fm);
            mArgs = new Bundle();
            mCursor = cursor;
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public Fragment getItem(int position) {
            FeedViewFragment f = new FeedViewFragment();
            mArgs.putParcelable("feed_uri", mFeeds.getFeedUri(position));
            f.setArguments(mArgs);
            return f;
        }

        
    }
}
