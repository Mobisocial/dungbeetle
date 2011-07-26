package edu.stanford.mobisocial.dungbeetle;
import java.util.List;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public interface IdentityProvider {
	public String userName();
	public String userEmail();
	public String userProfile();
	public RSAPublicKey userPublicKey();
	public RSAPrivateKey userPrivateKey();
	public String userPersonId();
	public RSAPublicKey publicKeyForPersonId(String id);
	public List<RSAPublicKey> publicKeysForContactIds(List<Long> ids);
	public String personIdForPublicKey(RSAPublicKey key);
}
