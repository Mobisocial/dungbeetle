package edu.stanford.mobisocial.dungbeetle.feed.iface;

import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;

public interface OutgoingMessageHandler {
    String getType();
	Pair<JSONObject, byte[]> handleOutgoing(JSONObject json);

}
