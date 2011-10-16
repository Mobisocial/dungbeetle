package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public class IteratorObjHandler extends ObjHandler {
    private final List<IObjHandler> mHandlers = new ArrayList<IObjHandler>();

    public synchronized void addHandler(IObjHandler handler) {
        mHandlers.add(handler);
    }

    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        for (IObjHandler h : mHandlers) {
            h.handleObj(context, handler, obj);
        }
    }
}
