package edu.stanford.mobisocial.dungbeetle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver to start the DungBeetle service on boot of phone.
 */
public class StartupIntentReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, DungBeetleService.class));
    }
}