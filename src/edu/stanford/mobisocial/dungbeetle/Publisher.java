package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Publisher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: security
        AppReference app = AppReference.fromIntent(intent);
        Uri feed = intent.getParcelableExtra("mobisocial.db.FEED");
        // TODO: disabled due spam. Need "replaces" field for objects.
        Helpers.sendToFeed(context, app, feed);
    }

}
