package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.presence.InterruptMePresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;

public class IteratorObjHandler extends ObjHandler {
    private final List<IObjHandler> mHandlers = new ArrayList<IObjHandler>();

    public static IteratorObjHandler getFromNetworkHandlers() {
        IteratorObjHandler h = new IteratorObjHandler();
        h.addHandler(InterruptMePresence.getInstance());
        h.addHandler(Push2TalkPresence.getInstance());
        return h;
    }
    public synchronized void addHandler(IObjHandler handler) {
        mHandlers.add(handler);
    }

    @Override
    public synchronized void handleObj(Context context, Uri feedUri, long contactId, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        for (IObjHandler h : mHandlers) {
            h.handleObj(context, feedUri, contactId, sequenceId, typeInfo, json);
        }
    }
}
