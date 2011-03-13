package edu.stanford.mobisocial.dungbeetle;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBIdentityProvider implements IdentityProvider {

    public static final String TAG = "DBIdentityProvider";
	private PublicKey mPubKey;
	private String mPubKeyTag;
	private PrivateKey mPrivKey;
	private SQLiteOpenHelper mDb;

	public DBIdentityProvider(SQLiteOpenHelper db) {
        mDb = db;
        mPubKey = getMyPubKey(db);
        mPrivKey = getMyPrivKey(db);
        mPubKeyTag = personIdForPublicKey(mPubKey);
    }

	public PublicKey userPublicKey(){
        return mPubKey;
    }

	public PrivateKey userPrivateKey(){
        return mPrivKey;
    }

	public String userPersonId(){
        return mPubKeyTag;
    }

	public PublicKey publicKeyForPersonId(String id){
        Cursor c = mDb.getReadableDatabase().rawQuery(
            "SELECT public_key FROM contacts WHERE person_id = ?",
            new String[] {id});
        c.moveToFirst();
        if(c.isAfterLast()){
            return null;
        }
        else{
            return publicKeyFromString(c.getString(0));
        }
    }

    public String personIdForPublicKey(PublicKey key){
        return makePersonIdForPublicKey(key);
    }

    public static String makePersonIdForPublicKey(PublicKey key){
		String me = null;
		try {
			me = Util.SHA1(key.getEncoded());
		} catch (Exception e) {
			throw new IllegalArgumentException(
                "Could not compute SHA1 of public key.");
		}
		return me.substring(0, 10);
    }


    public static void generateAndStoreKeys(SQLiteOpenHelper db){
        try {
            // Generate a 1024-bit Digital Signature Algorithm (RSA) key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keypair = keyGen.genKeyPair();
            PrivateKey privateKey = keypair.getPrivate();
            PublicKey publicKey = keypair.getPublic();

            String pubKeyStr = Base64.encodeToString(publicKey.getEncoded(), false);
            String privKeyStr = Base64.encodeToString(privateKey.getEncoded(), false);

            ContentValues cv = new ContentValues();
            cv.put("public_key", pubKeyStr);
            cv.put("private_key", privKeyStr);
            db.getWritableDatabase().insertOrThrow("my_info", null, cv);

            Log.d(TAG, "Generated public key: " + pubKeyStr);
            Log.d(TAG, "Generated priv key: " + privKeyStr);

        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate key pair! " + e);
        }
    }

    public static PublicKey publicKeyFromString(String str){
        try{
            byte[] pubKeyBytes = Base64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);                
        }
        catch(Exception e){
            throw new IllegalStateException("Error loading public key: " + e);
        }
    }

    public static PrivateKey privateKeyFromString(String str){
        try{
            byte[] privKeyBytes = Base64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        }
        catch(Exception e){
            throw new IllegalStateException("Error loading public key: " + e);
        }
    }


	private static PublicKey getMyPubKey(SQLiteOpenHelper db) {
		Cursor c = db.getReadableDatabase().rawQuery("SELECT public_key FROM my_info", new String[] {});
		c.moveToFirst();
        if(c.isAfterLast()){
            throw new IllegalStateException("Missing my_info entry!");
        }
        else{
            String pubKeyStr = c.getString(0);
            Log.d(TAG, "Loaded public key: " + pubKeyStr);
            return publicKeyFromString(pubKeyStr);
        }
	}

	private static PrivateKey getMyPrivKey(SQLiteOpenHelper db) {
		Cursor c = db.getReadableDatabase().rawQuery("SELECT private_key FROM my_info", new String[] {});
		c.moveToFirst();
        if(c.isAfterLast()){
            throw new IllegalStateException("Missing my_info entry!");
        }
        else{
            try{
                String privKeyStr = c.getString(0);
                Log.d(TAG, "Loaded private key: " + privKeyStr);
                return privateKeyFromString(privKeyStr);
            }
            catch(Exception e){
                throw new IllegalStateException("Error loading private key: " + e);
            }
        }
	}


}
