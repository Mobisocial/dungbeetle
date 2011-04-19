package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.ArrayList;
import java.util.List;

public final class ObjectReceiverManager {
	public static List<ObjectReceiver> getDefaults() {
		List<ObjectReceiver> receivers = new ArrayList<ObjectReceiver>();
		receivers.add(new StatusUpdate());
		return receivers;
	}
}
