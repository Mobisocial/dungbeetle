package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Publisher extends BroadcastReceiver {
    public static final String EXTRA_APP_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String arg = intent.getStringExtra(EXTRA_APP_ARGUMENT);
        // TODO: security
        String pkg = intent.getStringExtra("mobisocial.db.PKG");
        String state = intent.getStringExtra("mobisocial.db.STATE");
        AppReference app = new AppReference(pkg, arg, state);
        Uri feed = intent.getParcelableExtra("mobisocial.db.FEED");
        // TODO: disabled due to bugs.
        //Helpers.sendToFeed(context, app, feed);
    }

}
