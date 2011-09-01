package edu.stanford.mobisocial.dungbeetle.obj.handler;

import org.json.JSONObject;

import android.net.Uri;

public abstract class ObjHandler {

    public abstract void handleObj(Uri feedUri, long contactId, long sequenceId, String type, JSONObject json);
}
