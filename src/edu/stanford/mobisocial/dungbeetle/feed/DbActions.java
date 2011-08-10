package edu.stanford.mobisocial.dungbeetle.feed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.CameraAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.LaunchApplicationAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.LivePhotosAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.MusicAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.NewFeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.VoiceAction;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;

public class DbActions {

    // TODO: Use reflection.
    private static final List<FeedAction> sFeedActions = new ArrayList<FeedAction>();
    static {
        sFeedActions.add(new MusicAction());
        sFeedActions.add(new LivePhotosAction());
        sFeedActions.add(new CameraAction());
        sFeedActions.add(new VoiceAction());
        sFeedActions.add(new NewFeedAction());
        sFeedActions.add(new LaunchApplicationAction());
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
