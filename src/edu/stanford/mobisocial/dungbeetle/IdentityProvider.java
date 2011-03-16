package edu.stanford.mobisocial.dungbeetle;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface IdentityProvider {
	public String userName();
	public String userEmail();
	public PublicKey userPublicKey();
	public PrivateKey userPrivateKey();
	public String userPersonId();
	public PublicKey publicKeyForPersonId(String id);
	public String personIdForPublicKey(PublicKey key);
}
