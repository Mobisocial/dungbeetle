package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public interface IObjHandler {
    public abstract void handleObj(Context context, Uri feedUri,
            Contact contact, long sequenceId, DbEntryHandler typeInfo, JSONObject json, byte[] raw);
}
