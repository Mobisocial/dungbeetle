package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Publisher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: security
        AppReference app = AppReference.fromIntent(intent);
        Uri feed = intent.getParcelableExtra(AppReference.EXTRA_FEED_URI);
        // TODO: disabled due spam. Need "replaces" field for objects.
        //Helpers.sendToFeed(context, app, feed);
        Log.d("MUSUBI_FAIL", "Not sending from publisher, this is dead code.");
    }

}
