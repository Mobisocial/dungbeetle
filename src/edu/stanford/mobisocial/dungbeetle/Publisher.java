package edu.stanford.mobisocial.dungbeetle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;

public class Publisher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppReference app = AppReference.fromIntent(intent);
        Uri feed = intent.getParcelableExtra(AppReference.EXTRA_FEED_URI);
        Helpers.sendToFeed(context, app, feed);
    }
}
