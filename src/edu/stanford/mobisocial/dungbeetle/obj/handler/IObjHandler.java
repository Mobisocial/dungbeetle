package edu.stanford.mobisocial.dungbeetle.obj.handler;

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

public interface IObjHandler {
    public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj);
}
