package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.ui.FeedViewActivity;

public class VoiceAction implements FeedAction {

    @Override
    public String getName() {
        return "Voice";
    }

    @Override
    public void onClick(Context context, Uri feedUri) {
        Intent voiceintent = new Intent(context, VoiceRecorderActivity.class);
        voiceintent.putExtra("feedUri", feedUri.toString());
        context.startActivity(voiceintent);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
