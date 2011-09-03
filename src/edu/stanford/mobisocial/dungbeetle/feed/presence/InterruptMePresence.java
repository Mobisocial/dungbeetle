package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IObjHandler;

public class InterruptMePresence extends FeedPresence implements IObjHandler {
    private static final String TAG = "interrupt";
    private boolean mInterrupt = false;
    private static InterruptMePresence sInstance;

    private InterruptMePresence() {

    }

    @Override
    public String getName() {
        return "Interrupt Me";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        mInterrupt = present;
    }

    public static InterruptMePresence getInstance() {
        if (sInstance == null) {
            sInstance = new InterruptMePresence();
        }
        return sInstance;
    }

    @Override
    public void handleObj(Context context, Uri feedUri, long contactId, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        if (mInterrupt) {
            if (typeInfo instanceof Activator) {
                ((Activator) typeInfo).activate(context, json);
            }
        }
    }
}
