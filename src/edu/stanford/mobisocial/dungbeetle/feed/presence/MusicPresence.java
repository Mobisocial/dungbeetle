package edu.stanford.mobisocial.dungbeetle.feed.presence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;

public class MusicPresence extends FeedPresence {
    private boolean mShareMusic = false;

    @Override
    public String getName() {
        return "Music";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mShareMusic) {
            if (getFeedsWithPresence().size() == 0) {
                context.getApplicationContext().unregisterReceiver(mReceiver);
                Toast.makeText(context, "No longer sharing music", Toast.LENGTH_SHORT).show();
                mShareMusic = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                IntentFilter iF = new IntentFilter();
                iF.addAction("com.android.music.metachanged");
                context.getApplicationContext().registerReceiver(mReceiver, iF);
                Toast.makeText(context, "Now sharing music", Toast.LENGTH_SHORT).show();
                mShareMusic = true;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mShareMusic) {
                String artist = intent.getStringExtra("artist");
                // String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                String song = artist + " - " + track;
                for (Uri feedUri : getFeedsWithPresence()) {
                    Helpers.sendToFeed(context.getApplicationContext(), StatusObj.from(song), feedUri);
                }
            }
        }
    };
}
