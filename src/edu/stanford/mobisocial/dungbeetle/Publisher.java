package edu.stanford.mobisocial.dungbeetle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.AppState;

@Deprecated
public class Publisher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppState app = AppState.fromIntent(intent);
        Uri feed = intent.getParcelableExtra(AppState.EXTRA_FEED_URI);
        Helpers.sendToFeed(context, app, feed);
    }
}
