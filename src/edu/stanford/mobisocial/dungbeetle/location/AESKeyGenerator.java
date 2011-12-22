/**
 * Generates different keys (k_1, k_2, etc.) from a shared Key (K) between two clients.
 * Also can be used to generate the random value, r, between the server and the client.
 * 
 * Uses the shared key K that is associated with each friend and the time that is passed in as one of the parameters.
 */
package edu.stanford.mobisocial.dungbeetle.location;

import java.math.BigInteger;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class AESKeyGenerator {
	
	Cipher mCipher;

	public AESKeyGenerator()
	{
		// default constructor
	}
	
	/**The only public function and is *the* function of this class. It generates the k_1, k_2, etc. from 
	 * parameters Friend, f, and time or whatever it is you want to "encrypt", toEncrypt.
	 * 
	 */ 
	public byte[] generate_k(String dhkey, String toEncrypt)
	{
		byte[] retVal;
		
		try { // Set up the Cipher class of Android to use AES to generate keys
			mCipher = Cipher.getInstance("AES");
			//Log.d("Encryption Log", "Created an uninitialized instance AES cipher successfully.");
			// Set up key to use in algorithm
			MessageDigest hasher = MessageDigest.getInstance("SHA-256"); // Initialize object that will hash my key.
			//Log.d("Encryption Log", "Created SHA-256 hasher successfully.");
			byte[] key256 = hasher.digest(dhkey.getBytes()); // Hash the key to 256 bits using SHA
			
			//Log.d("Encryption Log", "Key hashed sucessfully.");
			SecretKeySpec K = new SecretKeySpec(key256, "AES");
			//Log.d("Encryption Log", "Secret key spec created successfully.");
			mCipher.init(Cipher.ENCRYPT_MODE, K);
			//Log.d("Encryption Log", "Cipher initialized correctly to encrypt using K.");
		// Encrypt the parameter toEncrypt
		try { 
			retVal = mCipher.doFinal(toEncrypt.getBytes());
			//Log.d("Encryption Log", "Encrypted \"message\" successfully, so now a key has been generated. The key pointer is: " + retVal.toString());
			
			return retVal;
		}
		catch (Exception e)
		{
			//System.err.println("Was able to create an object Cipher but could not encrypt the bytes");
		}
		
		}
		catch (Exception e) {
			//System.err.println("Could not create and initialize object Cipher.");
		}
		
		return null;
		
	}
	/**The only public function and is *the* function of this class. It generates the k_1, k_2, etc. from 
	 * parameters Friend, f, and time or whatever it is you want to "encrypt", toEncrypt.
	 * 
	 */
	public byte[] generate_r(byte[] sharedKey, String toEncrypt)
	{
		byte[] retVal;
		
		try {
			/*byte[] iv = new byte[16];
			for (int i = 0; i < iv.length; i++) 
				iv[i] = new Byte("0").byteValue();
			IvParameterSpec ivspec = new IvParameterSpec(iv);*/
			// Set up the Cipher class of Android to use AES to generate keys
			mCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			//Log.d("Encryption Log", "Created an uninitialized instance AES cipher successfully.");
			// Set up key to use in algorithm
			MessageDigest hasher = MessageDigest.getInstance("SHA-256"); // Initialize object that will hash my key.
			//Log.d("Encryption Log", "Created SHA-256 hasher successfully.");
			
			System.err.println("server key is: " + sharedKey);
			byte[] key256 = hasher.digest(sharedKey); // Hash the key to 256 bits using SHA
			//Log.d("Encryption Log", "Hashed random value key: " + new BigInteger(key256).toString());
			//Log.d("Encryption Log", "Key hashed sucessfully.");
			SecretKeySpec K = new SecretKeySpec(key256, "AES");
			//Log.d("Encryption Log", "Secret key spec created successfully.");
			mCipher.init(Cipher.ENCRYPT_MODE, K);
			//Log.d("Encryption Log", "Cipher initialized correctly to encrypt using shared server key.");
		// Encrypt the parameter toEncrypt
		try { 
			retVal = mCipher.doFinal(toEncrypt.getBytes());
			//Log.d("Encryption Log", "Encrypted \"message\" successfully, so now a key has been generated. The key pointer is: " + retVal.toString());
			
			return retVal;
		}
		catch (Exception e)
		{
			//System.err.println("Was able to create an object Cipher but could not encrypt the bytes");
		}
		
		}
		catch (Exception e) {
			//System.err.println(e.toString());
		}
		
		return null;
		
	}
}
