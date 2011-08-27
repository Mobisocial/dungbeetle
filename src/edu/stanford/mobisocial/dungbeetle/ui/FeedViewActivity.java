package edu.stanford.mobisocial.dungbeetle.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class FeedViewActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_view);
        getSupportFragmentManager().beginTransaction()
            .add(R.id.frame, new FeedViewFragment()).commit();
    }
}


