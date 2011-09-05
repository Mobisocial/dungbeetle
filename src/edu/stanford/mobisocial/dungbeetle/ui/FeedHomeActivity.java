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
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.view.FeedViews;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedActionsFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

/**
 * Represents a group by showing its feed and members.
 * TODO: Accept only a group_id extra and query for other parameters.
 */
public class FeedHomeActivity extends DashboardBaseActivity
        implements ViewPager.OnPageChangeListener, FeedListFragment.OnFeedSelectedListener {
    private Nfc mNfc;
    private String mGroupName;
    private FeedActionsFragment mActionsFragment;
    private Uri mFeedUri;
    private int mColor;
    private ViewPager mFeedViewPager;
    private final List<Button> mButtons = new ArrayList<Button>();

    private List<FeedView> mFeedViews = new ArrayList<FeedView>();

    public final String TAG = "GroupsTabActivity";

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

        mFeedViews = FeedViews.getDefaultFeedViews();

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        Intent intent = getIntent();
        Long group_id = null;
        String feed_name = null;
        String dyn_feed_uri = null;
        // TODO: Depracate extras-based access in favor of Data field.
        if (intent.hasExtra("group_id")) {
            group_id = intent.getLongExtra("group_id", -1);
            mGroupName = intent.getStringExtra("group_name");
            feed_name = mGroupName;
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                feed_name = g.feedName;
            } catch (Exception e) {}
            dyn_feed_uri = intent.getStringExtra("group_uri");
        } else if (getIntent().getType() != null && getIntent().getType().equals(Group.MIME_TYPE)) {
            group_id = Long.parseLong(getIntent().getData().getLastPathSegment());
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                mGroupName = g.name;
                feed_name = g.feedName;
                dyn_feed_uri = g.dynUpdateUri;
                group_id = g.id;
            } catch (Exception e) {}
        } else if (getIntent().getType() != null && getIntent().getType().equals(Feed.MIME_TYPE)) {
            Uri feedUri = getIntent().getData();
            Maybe<Group> maybeG = Group.forFeed(FeedHomeActivity.this, feedUri.getLastPathSegment());
            try {
                Group g = maybeG.get();
                mGroupName = g.name;
                feed_name = g.feedName;
                dyn_feed_uri = g.dynUpdateUri;
                group_id = g.id;
            } catch (Exception e) {}
        } else if (getIntent().getData().getAuthority().equals("vnd.mobisocial.db")) {
            String feedName = getIntent().getData().getLastPathSegment();
            Maybe<Group>maybeG = Group.forFeed(this, feedName);
            Group g = null;
            try {
               g = maybeG.get();
            } catch (Exception e) {
                g = Group.createForFeed(this, feedName);
            }
            mGroupName = g.name;
            feed_name = g.feedName;
            dyn_feed_uri = g.dynUpdateUri;
            group_id = g.id;
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

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (getSupportFragmentManager().findFragmentByTag("feedActions") == null) {
            // first run.
            ft.add(mActionsFragment, "feedActions");
            ft.commit();
        }

        PagerAdapter adapter = new FeedFragmentAdapter(getSupportFragmentManager(), mFeedUri);
        mFeedViewPager = (ViewPager)findViewById(R.id.feed_pager);
        mFeedViewPager.setAdapter(adapter);
        mFeedViewPager.setOnPageChangeListener(this);
        mFeedViewPager.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        new FeedFragmentAdapter(getSupportFragmentManager(), mFeedUri);
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
        setFeedUri(mFeedUri);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause(this);
        clearFeedUri();
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
}
