package edu.stanford.mobisocial.dungbeetle.transport;

import java.security.PublicKey;

public interface OutgoingMessage {
	public PublicKey toPublicKey();

	public String contents();
}
