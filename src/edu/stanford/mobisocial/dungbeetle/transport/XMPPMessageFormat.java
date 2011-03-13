package edu.stanford.mobisocial.dungbeetle.transport;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.util.*;
import java.security.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;

import javax.crypto.*;

public class XMPPMessageFormat {

    public static final int AES_Key_Size = 128;
    private IdentityProvider mIdent;

	public XMPPMessageFormat(IdentityProvider ident) {
        mIdent = ident;
	}

	public String getMessagePersonId(String s) {
        try{
            String[] parts = s.split(",");
            return new String(Base64.decode(parts[0]), "UTF8");
        }catch(UnsupportedEncodingException e){ return null; }
	}


	public String prepareIncomingMessage(String s, PublicKey sender) throws CryptoException{
		try {
			String[] parts = s.split(",");
			String aesKeyS = parts[1];
			byte[] aesKeyBytes = Base64.decode(aesKeyS);
			String sigS = parts[2];
			byte[] sigBytes = Base64.decode(sigS);
			String ivS = parts[3];
			byte[] ivBytes = Base64.decode(ivS);
			String ciphS = parts[4];
			byte[] ciphBytes = Base64.decode(ciphS);

            // Verify signature on AES key
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(sender);
			signature.update(aesKeyBytes);
            boolean status = signature.verify(sigBytes);
            if(!status){
                throw new CryptoException();
            }
			System.out.println("Verified signature on AES key!");

            // Decrypt AES key
			Cipher keyCipher = Cipher.getInstance("RSA");
            keyCipher.init(Cipher.DECRYPT_MODE, mIdent.userPrivateKey());
            CipherInputStream is = new CipherInputStream(
                new ByteArrayInputStream(aesKeyBytes), keyCipher);
            byte[] aesKey = new byte[AES_Key_Size/8];
            is.read(aesKey);
            is.close();
            SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");
			System.out.println("Decrypted AES key");


            // Use AES key to decrypt the body
            IvParameterSpec ivspec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aeskeySpec, ivspec);
            is = new CipherInputStream(
                new ByteArrayInputStream(ciphBytes), cipher);
            ByteArrayOutputStream plainOut = new ByteArrayOutputStream();
            Util.copy(is, plainOut);
            is.close();
			byte[] plainBytes = plainOut.toByteArray();


			return new String(plainBytes, "UTF8");

		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new CryptoException();
		}
	}

	public String prepareOutgoingMessage(String s, PublicKey toPubKey)
        throws CryptoException {
		try {
			byte[] plain = s.getBytes("UTF8");
            byte[] aesKey = makeAESKey();
            SecretKeySpec aesSpec = new SecretKeySpec(aesKey, "AES");

            // Encrypt the AES key with RSA
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, toPubKey);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CipherOutputStream os = new CipherOutputStream(out, cipher);
            os.write(aesKey);
            os.close();
            byte[] aesKeyCipherBytes = out.toByteArray();

            // Generate a signature of the AES key
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(mIdent.userPrivateKey(), new SecureRandom());
			signature.update(aesKeyCipherBytes);
			byte[] sigBytes = signature.sign();
			System.out.println("Computed signature of length " + sigBytes.length);

            // Generate Initialization Vector for AES CBC mode
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            // Use AES key to encrypt the body
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesSpec, ivspec);
            ByteArrayOutputStream cipherOut = new ByteArrayOutputStream();
            CipherOutputStream aesOut = new CipherOutputStream(cipherOut, aesCipher);
            aesOut.write(plain);
            aesOut.close();
			byte[] cipherData = cipherOut.toByteArray();
			System.out.println("Computed cipher of length " + cipherData.length);

			byte[] personIdBytes = mIdent.userPersonId().getBytes("UTF8");

			return (Base64.encodeToString(personIdBytes, false) + "," + 
                    Base64.encodeToString(aesKeyCipherBytes, false) + "," +
                    Base64.encodeToString(sigBytes, false) + "," + 
                    Base64.encodeToString(iv, false) + "," +
                    Base64.encodeToString(cipherData, false));

		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new CryptoException();
		}
	}


    /**
     * Creates a new AES key
     */
	private byte[] makeAESKey() throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    kgen.init(AES_Key_Size);
	    SecretKey key = kgen.generateKey();
	    return key.getEncoded();
	}
}
