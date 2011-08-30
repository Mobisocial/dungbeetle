package edu.stanford.mobisocial.dungbeetle.feed;

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
import edu.stanford.mobisocial.dungbeetle.feed.view.DefaultView;
import edu.stanford.mobisocial.dungbeetle.feed.view.PartyView;
import edu.stanford.mobisocial.dungbeetle.feed.view.MapView;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMapFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedMembersFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class DbViews {
    private static final List<FeedView> sFeedViews = new ArrayList<FeedView>();
    static {
        sFeedViews.add(new DefaultView());
        sFeedViews.add(new PartyView());
        sFeedViews.add(new MapView());
        //sFeedViews.add(new StatsView());
    }

    public static List<FeedView> getFeedViews() {
        return sFeedViews;
    }

    public static void promptForView(final Context context, final Uri feedUri) {
        String[] items = new String[DbViews.getFeedViews().size()];
        int i = 0;
        for (FeedView v : DbViews.getFeedViews()) {
            items[i++] = v.getName();
        }

        new AlertDialog.Builder(context)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FeedView v = DbViews.getFeedViews().get(which);
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

    public static List<FeedView> getDefaultFeedViews() {
        List<FeedView> feedViews = new ArrayList<FeedView>();
        feedViews.add(DbViews.feedViewFrom("Feed", new FeedViewFragment()));
        feedViews.add(DbViews.feedViewFrom("Map", new FeedMapFragment()));
        feedViews.add(DbViews.feedViewFrom("Members", new FeedMembersFragment()));
        return feedViews;
    }
}
