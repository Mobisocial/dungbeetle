package edu.stanford.mobisocial.dungbeetle.network;

import android.content.Intent;

public class RemoteIntent extends Intent {
    public static final String ACTION_REMOTE = "mobisocial.intent.action.REMOTE";
    public static final String EXTRA_ORIGINAL_INTENT = "original";
    public RemoteIntent(Intent original) {
        setAction(ACTION_REMOTE);
        putExtra(EXTRA_ORIGINAL_INTENT, original);
    }
}
