package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbViews;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class ViewAction implements FeedAction {

    public ViewAction() {
    }

    @Override
    public String getName() {
        return "View";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
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
                            .replace(R.id.frame, f).commit();
                }
            })
            .setTitle("View...")
            .create().show();
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
