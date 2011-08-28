package edu.stanford.mobisocial.dungbeetle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

public class FeedViewActivity extends FragmentActivity implements InstrumentedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_view);
        getSupportFragmentManager().beginTransaction()
            .add(R.id.frame, new FeedViewFragment()).commit();
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


