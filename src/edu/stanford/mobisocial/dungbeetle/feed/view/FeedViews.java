package edu.stanford.mobisocial.dungbeetle.feed.view;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMembersFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class FeedViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new DefaultView());
        sFeedViews.add(FeedViews.feedViewFrom("Members", new FeedMembersFragment()));
        sFeedViews.add(new MapView());
        sFeedViews.add(new PartyView());
        sFeedViews.add(new PresenceView());
        //sFeedViews.add(new StatsView());
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }

    public static List<FeedView> getDefaultFeedViews() {
        List<FeedView> feedViews = new ArrayList<FeedView>();
        feedViews.add(FeedViews.feedViewFrom("Feed", new FeedViewFragment()));
        feedViews.add(FeedViews.feedViewFrom("Members", new FeedMembersFragment()));
        //feedViews.add(FeedViews.feedViewFrom("Map", new FeedMapFragment()));
        //feedViews.add(FeedViews.feedViewFrom("Members", new FeedMembersFragment()));
        if (MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            feedViews.add(new PresenceView());
        }
        return feedViews;
    }

    public static void promptForView(final Context context, final Uri feedUri) {
        String[] items = new String[getFeedViews().size()];
        int i = 0;
        for (FeedView v : getFeedViews()) {
            items[i++] = v.getName();
        }

        new AlertDialog.Builder(context)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FeedView v = FeedViews.getFeedViews().get(which);
                    Fragment f = v.getFragment();
                    Bundle args = new Bundle();
                    args.putParcelable("feed_uri", feedUri);
                    f.setArguments(args);
                    ((FragmentActivity)context).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.feed_view, f)
                            .addToBackStack(null).commit();
                }
            })
            .setTitle("View...")
            .create().show();
    }

    public static FeedView feedViewFrom(final String name, final Fragment fragment) {
        return new FeedView() {
            @Override
            public Fragment getFragment() {
                return fragment;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}