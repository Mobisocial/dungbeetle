package edu.stanford.mobisocial.dungbeetle.feed.presence;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * Sends messages rapidly. For testing, not annoying friends!
 *
 */
public class SpamPresence extends FeedPresence {
    private boolean mShareSpam = false;
    private SpamThread mSpamThread;

    @Override
    public String getName() {
        return "Spam";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mShareSpam) {
            if (getFeedsWithPresence().size() == 0) {
                Toast.makeText(context, "No longer spamming", Toast.LENGTH_SHORT).show();
                mShareSpam = false;
                mSpamThread = null;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                Toast.makeText(context, "Now spamming your friends", Toast.LENGTH_SHORT).show();
                mShareSpam = true;
                mSpamThread = new SpamThread(context);
                mSpamThread.start();
            }
        }
    }

    class SpamThread extends Thread {
        long WAIT_TIME = 500;
        final Context mContext;

        public SpamThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            DbObject obj = StatusObj.from("StatusObj spam, StatusObj spam.");
            while (mShareSpam) {
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {}

                if (!mShareSpam) {
                    break;
                }

                Helpers.sendToFeeds(mContext, obj, getFeedsWithPresence());
            }
        }
    }
}