
package edu.stanford.mobisocial.dungbeetle.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import edu.stanford.mobisocial.dungbeetle.AboutActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.SearchActivity;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedActionsFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

/**
 * Displays a list of all user-accessible threads (feeds).
 */
public class FeedListActivity extends DashboardBaseActivity implements
        FeedListFragment.OnFeedSelectedListener {

    private static final String FRAGMENT_FEED_ACTIONS = "feedActions";
    private boolean mDualPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_list);
        DashboardBaseActivity.doTitleBar(this);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.feed_list, new FeedListFragment()).commit();
        mDualPane = (null != findViewById(R.id.feed_view));
    }

    @Override
    public void onFeedSelected(Uri feedUri) {
        if (mDualPane) {
            Bundle args = new Bundle();
            args.putParcelable(FeedViewFragment.ARG_FEED_URI, feedUri);
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
            Intent launch = new Intent(Intent.ACTION_VIEW);
            launch.setDataAndType(feedUri, Feed.MIME_TYPE);
            startActivity(launch);
        }
    }
}
