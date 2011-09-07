package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IObjHandler;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

/**
 * Automatically launches all openable content.
 */
public class TVModePresence extends FeedPresence implements IObjHandler {
    private static final String TAG = "interrupt";
    private boolean mInterrupt = false;
    private static TVModePresence sInstance;

    private TVModePresence() {

    }

    @Override
    public String getName() {
        return "TV Mode";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        mInterrupt = getFeedsWithPresence().size() > 0;
    }

    public static TVModePresence getInstance() {
        if (sInstance == null) {
            sInstance = new TVModePresence();
        }
        return sInstance;
    }

    @Override
    public void handleObj(Context context, Uri feedUri, Contact contact, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        if (mInterrupt) {
            if (typeInfo instanceof Activator) {
                if (DBG) Log.d(TAG, "activatin via tv mode");
                ((Activator) typeInfo).activate(context, json);
            }
        }
    }
}
