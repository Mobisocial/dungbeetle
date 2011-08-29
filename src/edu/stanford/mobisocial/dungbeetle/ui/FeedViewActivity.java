package edu.stanford.mobisocial.dungbeetle.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewSelectorFragment;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

public class FeedViewActivity extends FragmentActivity implements InstrumentedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_view);
        Fragment feedView = new FeedViewFragment();
        Fragment feedViewSelector = new FeedViewSelectorFragment();
        Bundle args = new Bundle();
        args.putParcelable("feed_uri", getFeedUri());
        feedView.setArguments(args);
        feedViewSelector.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .add(feedViewSelector, "viewSelector")
            .add(R.id.feed_view, feedView).commit();
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

    private Uri getFeedUri() {
        Intent intent = getIntent();
        if (intent.hasExtra("group_id")) {
            try {
                Long groupId = intent.getLongExtra("group_id", -1);
                DBHelper dbHelper = new DBHelper(this);
                String feedName = dbHelper.groupForGroupId(groupId).get().feedName;
                dbHelper.close();
                return Feed.uriForName(feedName);
            } catch (Maybe.NoValError e) {
                Log.e("musubi", "Tried to view a group with bad group id");
                return null;
            }
        } else if (intent.hasExtra("feed_id")) {
            return Feed.uriForName(intent.getStringExtra("feed_id"));
        }
        return Feed.uriForName("friend");
    }
}


