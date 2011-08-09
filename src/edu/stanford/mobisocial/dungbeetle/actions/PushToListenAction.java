package edu.stanford.mobisocial.dungbeetle.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.actions.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;

public class PushToListenAction implements FeedAction {
    private boolean mSharePhotos = false;
    private Context mContext;
    private Uri mFeedUri;

    @Override
    public String getName() {
        return "Push To Listen";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        boolean sharePhotos = !mSharePhotos;
        synchronized(this) {
            mFeedUri = feedUri;
            mContext = context.getApplicationContext();
            mSharePhotos = sharePhotos;
        }

        if (sharePhotos) {
            // music.setIcon(getResources().getDrawable(R.drawable.ic_menu_music_sharing));
            IntentFilter iF = new IntentFilter();
            iF.addAction("com.android.camera.NEW_PICTURE");
            mContext.registerReceiver(mReceiver, iF);

            Toast.makeText(context, "Now sharing new photos", Toast.LENGTH_SHORT).show();
        } else {
            // music.setIcon(getResources().getDrawable(R.drawable.ic_menu_music_not_sharing));
            mContext.unregisterReceiver(mReceiver);
            Toast.makeText(context, "No longer sharing photos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean isActive() {
        return false;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSharePhotos) {
                Toast.makeText(context, "click!", 5000).show();
                Helpers.sendToFeed(mContext, StatusObj.from("got a pic"), mFeedUri);
                /*
                String action = intent.getAction();
                String cmd = intent.getStringExtra("command");
                //Log.d("mIntentReceiver.onReceive ", action + " / " + cmd);
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                //Log.d("Music",artist+":"+album+":"+track);
                String song = artist + " - " + track;
                    Helpers.sendToFeed(mContext, StatusObj.from(song), mFeedUri);
                    */
            }
        }
    };
}
