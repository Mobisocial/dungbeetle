package edu.stanford.mobisocial.dungbeetle.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RemoteIntentCatcher extends BroadcastReceiver {
    private Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        handleRemoted(intent, true);
    }

    private void handleRemoted(Intent intent, boolean isLocal) {
        if (isLocal) {
            if (isOrderedBroadcast()) {
                mContext.sendOrderedBroadcast(intent, null);
            } else {
                
            }
        }
    }
}
