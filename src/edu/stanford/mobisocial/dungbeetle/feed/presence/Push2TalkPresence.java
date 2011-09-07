package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IObjHandler;

public class Push2TalkPresence extends FeedPresence implements IObjHandler {
    private static final String TAG = "push2talk";
    private boolean mEnabled = false;
    private static Push2TalkPresence sInstance;

    private Push2TalkPresence() {

    }

    @Override
    public String getName() {
        return "Push2Talk";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        mEnabled = getFeedsWithPresence().size() > 0;
    }

    public static Push2TalkPresence getInstance() {
        if (sInstance == null) {
            sInstance = new Push2TalkPresence();
        }
        return sInstance;
    }

    @Override
    public void handleObj(Context context, Uri feedUri, Contact contact, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        if (!mEnabled || !getFeedsWithPresence().contains(feedUri) ||
                !(typeInfo instanceof VoiceObj)) {
            return;
        }

        if (DBG) Log.d(TAG, "Playing audio via push2talk on " + feedUri);
        ((VoiceObj) typeInfo).activate(context, json);
    }

    public boolean isOnCall() {
        return mEnabled;
    }
}
