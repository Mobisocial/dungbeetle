package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.MusicObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

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
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                DbObject obj = MusicObj.from(artist, album, track);
                try {
                    if (intent.hasExtra("url")) {
                        obj.getJson().put(MusicObj.URL, intent.getStringExtra("url"));
                        if (intent.hasExtra("mimeType")) {
                            obj.getJson().put(MusicObj.MIME_TYPE,
                                    intent.getStringExtra("mimeType"));
                        }
                    }
                } catch (JSONException e) {}
                for (Uri feedUri : getFeedsWithPresence()) {
                    Helpers.sendToFeed(context.getApplicationContext(), obj, feedUri);
                }
            }
        }
    };
}
