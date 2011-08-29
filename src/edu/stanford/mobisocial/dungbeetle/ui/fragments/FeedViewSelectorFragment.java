package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import edu.stanford.mobisocial.dungbeetle.feed.DbViews;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * A UI-less fragment that adds a view selector to the menu.
 */
public class FeedViewSelectorFragment extends Fragment {
    private static final int MENU_VIEW = 1041;
    private Uri mFeedUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mFeedUri = getArguments().getParcelable("feed_uri");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(0, MENU_VIEW, 0, "View");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()) {
            case MENU_VIEW: {
                DbViews.promptForView(getActivity(), mFeedUri);
                return true;
            }
        }
        return false;
    }
}
