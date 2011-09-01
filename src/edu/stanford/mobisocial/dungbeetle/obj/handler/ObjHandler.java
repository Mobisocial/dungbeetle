package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;

public abstract class ObjHandler {

    public abstract void handleObj(Context context, Uri feedUri,
            long contactId, long sequenceId, String type, JSONObject json);
}
