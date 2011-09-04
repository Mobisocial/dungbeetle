package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    @SuppressWarnings("unused")
    private static final String TAG = "msb-remoteReceiver";
    private static RemoteControlReceiver sInstance;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            handleSpecialButton(context, (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
        }
    }

    public RemoteControlReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new RemoteControlReceiver();
        }
        return sInstance;
    }

    public void handleSpecialButton(Context context, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            if (Push2TalkPresence.getInstance().isOnCall()) {
                Intent record = new Intent();
                record.setClass(context, VoiceRecorderActivity.class);
                record.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                record.putExtra("presence", true);
                context.startActivity(record);
            }
        }
    }
}