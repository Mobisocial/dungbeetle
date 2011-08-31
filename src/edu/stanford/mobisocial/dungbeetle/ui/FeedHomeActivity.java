package edu.stanford.mobisocial.dungbeetle.ui;

import java.util.ArrayList;
import java.util.List;

import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.AboutActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbViews;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedActionsFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

/**
 * Represents a group by showing its feed and members.
 * TODO: Accept only a group_id extra and query for other parameters.
 */
public class FeedHomeActivity extends FragmentActivity
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

    /*** Dashboard stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId, String title)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (title);
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }


    public void onClickBroadcast(View v) {
        mActionsFragment.promptForSharing();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActionsFragment.onActivityResult(requestCode, resultCode, data);
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_home);
        mNfc = new Nfc(this);

        mFeedViews = DbViews.getDefaultFeedViews();

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
        args.putParcelable("feed_uri", mFeedUri);
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

        new FeedFragmentAdapter(getSupportFragmentManager(), mFeedUri);
        mFeedViewPager = (ViewPager)findViewById(R.id.feed_pager);

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

        DashboardBaseActivity.doTitleBar(this, mGroupName);
        onPageSelected(0);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

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

    public void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FeedHomeActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
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
            mButtons.get(i).setBackgroundColor(R.color.background1);
        }
        mButtons.get(position).setBackgroundColor(mColor);
    }
}
