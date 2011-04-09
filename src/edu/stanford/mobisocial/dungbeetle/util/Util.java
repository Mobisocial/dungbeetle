package edu.stanford.mobisocial.dungbeetle.util;
import android.util.Log;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.math.BigInteger;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Util {

    public static final String TAG = "Util";

    /**
	 * Copies a stream.
	 */
	public static void copy(InputStream is, OutputStream os) throws IOException {
		int i;
		byte[] b = new byte[1024];
		while((i=is.read(b))!=-1) {
			os.write(b, 0, i);
		}
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;

			do {
				if ((0 <= halfbyte) && (halfbyte <= 9)) {
					buf.append((char) ('0' + halfbyte));
				} else {
					buf.append((char) ('a' + (halfbyte - 10)));
				}

				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}

		return buf.toString();
	}

	public static String SHA1(byte[] input) throws NoSuchAlgorithmException,
                                                   UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("SHA-1");

		byte[] sha1hash = new byte[40];
		md.update(input, 0, input.length);
		sha1hash = md.digest();

		return convertToHex(sha1hash);
	}

    public static String MD5(String plaintext){
        try{
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plaintext.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            String hashtext = bigInt.toString(16);
            while(hashtext.length() < 32 ){
                hashtext = "0"+hashtext;
            }
            return hashtext;
        }catch(Exception e){
            return null;
        }
    }

    public static byte[] newAESKey(){
        try{
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128); // 192 and 256 bits may not be available
            SecretKey skey = kgen.generateKey();
            return skey.getEncoded();
        }
        catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptAES(byte[] cipherText, byte[] key){
        // Use AES key to encrypt the body
        try{
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, skeySpec);
            ByteArrayOutputStream cipherOut = new ByteArrayOutputStream();
            CipherOutputStream aesOut = new CipherOutputStream(cipherOut, aesCipher);
            aesOut.write(cipherText);
            aesOut.close();
            return cipherOut.toByteArray();
        }
        catch(InvalidKeyException e){
            throw new RuntimeException(e);
        }
        catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
        catch(NoSuchPaddingException e){
            throw new RuntimeException(e);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptAES(byte[] plainText, byte[] key){
        try{
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            ByteArrayOutputStream cipherOut = new ByteArrayOutputStream();
            CipherOutputStream aesOut = new CipherOutputStream(cipherOut, aesCipher);
            aesOut.write(plainText);
            aesOut.close();
            return cipherOut.toByteArray();
        }
        catch(InvalidKeyException e){
            throw new RuntimeException(e);
        }
        catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
        catch(NoSuchPaddingException e){
            throw new RuntimeException(e);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static String encryptAES(String plainText, byte[] key){
        try{
            return Base64.encodeToString(encryptAES(plainText.getBytes("UTF8"), key), false);
        }
        catch(UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }

    public static String decryptAES(String b64CipherText, byte[] key){
        try{
            return new String(decryptAES(Base64.decode(b64CipherText), key), "UTF8");
        }
        catch(UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }

    public static String join(Collection<String> s, String delimiter) {
        if (s.isEmpty()) return "";
        Iterator<String> iter = s.iterator();
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter);
            buffer.append(iter.next());
        }
        return buffer.toString();
    }

    public static String joinLongs(Collection<Long> s, String delimiter) {
        if (s.isEmpty()) return "";
        Iterator<Long> iter = s.iterator();
        StringBuffer buffer = new StringBuffer(iter.next().toString());
        while (iter.hasNext()) {
            buffer.append(delimiter);
            buffer.append(iter.next());
        }
        return buffer.toString();
    }
}
