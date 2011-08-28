
package edu.stanford.mobisocial.dungbeetle.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.AboutActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.SearchActivity;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedListFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

/**
 * Displays a list of all user-accessible threads (feeds).
 */
public class FeedListActivity extends FragmentActivity
        implements FeedListFragment.OnFeedSelectedListener, InstrumentedActivity {

    private boolean mDualPane;

    public void goHome(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public void setTitleFromActivityLabel(int textViewId) {
        TextView tv = (TextView)findViewById(textViewId);
        if (tv != null) {
            tv.setText(getTitle());
        }
    }

    public void onClickHome(View v) {
        goHome(this);
    }

    public void onClickSearch(View v) {
        startActivity(new Intent(getApplicationContext(), SearchActivity.class));
    }

    public void onClickAbout(View v) {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_list);
        setTitleFromActivityLabel(R.id.title_text);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.feed_list, new FeedListFragment()).commit();
        mDualPane = (null != findViewById(R.id.feed_view));
    }

    @Override
    public void onFeedSelected(Uri feedUri) {
        if (mDualPane) {
            Bundle args = new Bundle();
            args.putParcelable(FeedViewFragment.ARG_FEED_URI, feedUri);
            FeedViewFragment feedView = new FeedViewFragment();
            feedView.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.feed_view, feedView).commit();  
        } else {
            Intent launch = new Intent(Intent.ACTION_VIEW);
            launch.setDataAndType(feedUri, Feed.MIME_TYPE);
            startActivity(launch);
        }
    }

    private static int ACTIVITY_CALLOUT = 39;
    private static ActivityCallout mCurrentCallout;
    public void doActivityForResult(ActivityCallout callout) {
        mCurrentCallout = callout;
        Intent launch = callout.getStartIntent();
        startActivityForResult(launch, ACTIVITY_CALLOUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_CALLOUT) {
            mCurrentCallout.handleResult(resultCode, data);
        }
    }
}
