package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

import android.content.Context;
import android.net.Uri;

public abstract class ObjHandler {

    public abstract void handleObj(Context context, Uri feedUri,
            long contactId, long sequenceId, DbEntryHandler typeInfo, JSONObject json);
}
