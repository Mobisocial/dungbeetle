package edu.stanford.mobisocial.dungbeetle.model;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.actions.CameraAction;
import edu.stanford.mobisocial.dungbeetle.actions.LaunchApplicationAction;
import edu.stanford.mobisocial.dungbeetle.actions.LivePhotosAction;
import edu.stanford.mobisocial.dungbeetle.actions.MusicAction;
import edu.stanford.mobisocial.dungbeetle.actions.NewFeedAction;
import edu.stanford.mobisocial.dungbeetle.actions.VoiceAction;
import edu.stanford.mobisocial.dungbeetle.actions.iface.FeedAction;

public class DbActions {

    // TODO: Use reflection.
    private static final List<FeedAction> sFeedActions = new ArrayList<FeedAction>();
    static {
        sFeedActions.add(new MusicAction());
        sFeedActions.add(new LivePhotosAction());
        sFeedActions.add(new CameraAction());
        sFeedActions.add(new LaunchApplicationAction());
        sFeedActions.add(new VoiceAction());
        sFeedActions.add(new NewFeedAction());
    }

    public static final QuickAction getActions(final Context c, final Uri feedUri, final View v) {
        QuickAction qa = new QuickAction(v);
        for (final FeedAction action : sFeedActions) {
            if (!action.isActive()) continue;

            final ActionItem item = new ActionItem();
            item.setTitle(action.getName());
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    action.onClick(c, feedUri);
                }
            });
            qa.addActionItem(item);
        }
        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
        return qa;
    }
}
