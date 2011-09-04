package edu.stanford.mobisocial.dungbeetle.feed.presence;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
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
        mEnabled = present;
    }

    public static Push2TalkPresence getInstance() {
        if (sInstance == null) {
            sInstance = new Push2TalkPresence();
        }
        return sInstance;
    }

    @Override
    public void handleObj(Context context, Uri feedUri, long contactId, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        if (mEnabled) {
            if (typeInfo instanceof VoiceObj) {
                ((VoiceObj) typeInfo).activate(context, json);
            }
        }
    }

    public boolean isOnCall() {
        return mEnabled;
    }
}
