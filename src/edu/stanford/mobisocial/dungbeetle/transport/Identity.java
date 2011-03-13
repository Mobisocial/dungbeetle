package edu.stanford.mobisocial.dungbeetle.transport;

import java.security.PublicKey;

public interface Identity {
	public PublicKey publicKey();

	public PublicKey getMessagePublicKey(String s);

	public String prepareIncomingMessage(String s, PublicKey sender) throws CryptoException;

	public String prepareOutgoingMessage(String s, PublicKey receiver)
			throws CryptoException;
}
