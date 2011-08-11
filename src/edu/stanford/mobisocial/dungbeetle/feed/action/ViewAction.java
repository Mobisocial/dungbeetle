package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.DbViews;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;

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
                    Intent intent = new Intent(context, v.getClassName());
                    intent.setData(feedUri);
                    context.startActivity(intent);
                }
            })
            .setTitle("View...")
            .create().show();
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
