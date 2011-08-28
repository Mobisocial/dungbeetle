package edu.stanford.mobisocial.dungbeetle.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbViews;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

public class AlternateFeedViewActivity extends FragmentActivity implements InstrumentedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_view);

        String[] items = new String[DbViews.getFeedViews().size()];
        int i = 0;
        for (FeedView v : DbViews.getFeedViews()) {
            items[i++] = v.getName();
        }

        new AlertDialog.Builder(this)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Uri feedUri = getFeedUri();
                    FeedView v = DbViews.getFeedViews().get(which);
                    Bundle args = new Bundle();
                    args.putParcelable("feed_uri", feedUri);
                    Fragment f = v.getFragment();
                    f.setArguments(args);
                    getSupportFragmentManager().beginTransaction().add(R.id.frame, f).commit();
                }
            })
            .setTitle("View...")
            .create().show();
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
        Uri feedUri;
        Intent intent = getIntent();
        if (intent.hasExtra("group_id")) {
            try {
                Long groupId = intent.getLongExtra("group_id", -1);
                DBHelper dbHelper = new DBHelper(this);
                String feedName = dbHelper.groupForGroupId(groupId).get().feedName;
                dbHelper.close();
                feedUri = Feed.uriForName(feedName);
            } catch (Maybe.NoValError e) {
                Log.wtf("db", "Tried to view a group with bad group id");
                feedUri = null; // java 'bug'; feedUri should be finalable ;(
            }
        } else if (intent.hasExtra("feed_id")) {
            feedUri = Feed.uriForName(intent.getStringExtra("feed_id"));
        } else {
            feedUri = null;
        }
        return feedUri;
    }
}


