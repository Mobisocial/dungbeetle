package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.handler.ObjHandler;

/**
 * Drop messages. Good for canning spam.
 *
 */
public class DropMessagesPresence extends FeedPresence {
    private boolean mDropMessages = false;
    private static DropMessagesPresence sInstance;

    // TODO: proper singleton.
    public static DropMessagesPresence getInstance() {
        if (sInstance == null) {
            sInstance = new DropMessagesPresence();
        }
        return sInstance;
    }

    private DropMessagesPresence() {}

    @Override
    public String getName() {
        return "Drop Messages";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mDropMessages) {
            if (getFeedsWithPresence().size() == 0) {
                Toast.makeText(context, "No longer ignoring your friends.", Toast.LENGTH_SHORT).show();
                mDropMessages = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                Toast.makeText(context, "Now ignoring your friends.", Toast.LENGTH_SHORT).show();
                mDropMessages = true;
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
            while (mDropMessages) {
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {}

                if (!mDropMessages) {
                    break;
                }

                Helpers.sendToFeeds(mContext, obj, getFeedsWithPresence());
            }
        }
    }

    public static class MessageDropHandler extends ObjHandler {

        public boolean preFiltersObj(Context context, Uri feedUri) {
            boolean a = getInstance().getFeedsWithPresence().contains(feedUri);
            Log.d(TAG, "CHECKING FILTER " + feedUri + ", " + a);
            return a;
        }

        @Override
        public void handleObj(Context context, Uri feedUri, Contact contact, long sequenceId,
                DbEntryHandler typeInfo, JSONObject json, byte[] raw) {
        }
    }
}
