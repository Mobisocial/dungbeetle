
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

/**
 * Displays a list of all user-accessible threads (feeds).
 */
public class FeedListActivity extends FragmentActivity
        implements FeedListFragment.OnFeedSelectedListener {
    private static final String TAG = "DungBeetle";

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
            .add(R.id.frame, new FeedListFragment()).commit();
    }

    @Override
    public void onFeedSelected(Uri feedUri) {
        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setDataAndType(feedUri, Feed.MIME_TYPE);
        startActivity(launch);
    }
}
