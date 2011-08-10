package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.FeedActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;

public class MusicAction implements FeedAction {
    private boolean mShareMusic = false;
    private Context mContext;
    private Uri mFeedUri;

    @Override
    public String getName() {
        return "Music";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        boolean shareMusic = !mShareMusic;
        synchronized(this) {
            mFeedUri = feedUri;
            mContext = context.getApplicationContext();
            mShareMusic = shareMusic;
        }

        if (shareMusic) {
            // music.setIcon(getResources().getDrawable(R.drawable.ic_menu_music_sharing));
            IntentFilter iF = new IntentFilter();
            iF.addAction("com.android.music.metachanged");
            // iF.addAction("com.android.music.playstatechanged");
            // iF.addAction("com.android.music.playbackcomplete");
            // iF.addAction("com.android.music.queuechanged");
            mContext.registerReceiver(mReceiver, iF);

            Toast.makeText(context, "Now sharing music", Toast.LENGTH_SHORT).show();
        } else {
            // music.setIcon(getResources().getDrawable(R.drawable.ic_menu_music_not_sharing));
            mContext.unregisterReceiver(mReceiver);
            Toast.makeText(context, "No longer sharing music", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mShareMusic) {
                String action = intent.getAction();
                String cmd = intent.getStringExtra("command");
                //Log.d("mIntentReceiver.onReceive ", action + " / " + cmd);
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                //Log.d("Music",artist+":"+album+":"+track);
                String song = artist + " - " + track;
                    Helpers.sendToFeed(mContext, StatusObj.from(song), mFeedUri);
            }
        }
    };
}
