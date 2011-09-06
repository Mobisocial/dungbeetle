
package edu.stanford.mobisocial.dungbeetle.ui;

import android.os.Bundle;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedHistoryFragment;

/**
 * Displays a list of all user-accessible threads (feeds).
 */
public class FeedHistoryActivity extends MusubiBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_list);
        MusubiBaseActivity.doTitleBar(this);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.feed_list, new FeedHistoryFragment()).commit();
    }
}
