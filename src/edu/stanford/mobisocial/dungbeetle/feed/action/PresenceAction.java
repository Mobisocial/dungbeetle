package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.DbPresence;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;

public class PresenceAction implements FeedAction {

    @Override
    public String getName() {
        return "Engage";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        List<FeedPresence> presenceTypes = DbPresence.getPresenceTypes();
        final String[] presence = new String[presenceTypes.size()];
        final boolean[] oldPresence = new boolean[presence.length];
        final boolean[] newPresence = new boolean[presence.length];
        int i = 0;
        for (FeedPresence p : presenceTypes) {
            oldPresence[i] = p.isPresent(feedUri);
            newPresence[i] = oldPresence[i];
            presence[i++] = p.getName();
        }
        new AlertDialog.Builder(context)
            .setMultiChoiceItems(presence, newPresence,
                    new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    newPresence[which] = isChecked;
                }
            })
            .setTitle("Sharing...")
            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (int i = 0; i < presence.length; i++) {
                        if (oldPresence[i] != newPresence[i]) {
                            DbPresence.getPresenceTypes().get(i).setFeedPresence(
                                    context, feedUri, newPresence[i]);
                        }
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
            .create().show();
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
