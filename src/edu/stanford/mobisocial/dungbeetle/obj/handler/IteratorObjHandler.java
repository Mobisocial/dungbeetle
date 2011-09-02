package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public class IteratorObjHandler extends ObjHandler {
    private final List<ObjHandler> mHandlers = new ArrayList<ObjHandler>();

    public synchronized void addHandler(ObjHandler handler) {
        mHandlers.add(handler);
    }

    @Override
    public synchronized void handleObj(Context context, Uri feedUri, long contactId, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        for (ObjHandler h : mHandlers) {
            h.handleObj(context, feedUri, contactId, sequenceId, typeInfo, json);
        }
    }
}
