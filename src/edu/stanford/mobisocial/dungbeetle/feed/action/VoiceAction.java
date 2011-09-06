package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.VoiceQuickRecordActivity;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class VoiceAction implements FeedAction { // TODO: Move to VoiceObj implements FeedAction

    @Override
    public String getName() {
        return "Voice";
    }

    @Override
    public void onClick(Context context, Uri feedUri) {
        Intent record = new Intent();
        record.setClass(context, VoiceRecorderActivity.class);
        record.putExtra(FeedViewFragment.ARG_FEED_URI, feedUri.toString());
        context.startActivity(record);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
