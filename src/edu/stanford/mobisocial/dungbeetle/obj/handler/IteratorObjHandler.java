package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class IteratorObjHandler extends ObjHandler {
    private final List<IObjHandler> mHandlers = new ArrayList<IObjHandler>();

    public synchronized void addHandler(IObjHandler handler) {
        mHandlers.add(handler);
    }

    @Override
    public synchronized void handleObj(Context context, Uri feedUri, Contact contact, long sequenceId,
            DbEntryHandler typeInfo, JSONObject json) {
        for (IObjHandler h : mHandlers) {
            h.handleObj(context, feedUri, contact, sequenceId, typeInfo, json);
        }
    }
}
