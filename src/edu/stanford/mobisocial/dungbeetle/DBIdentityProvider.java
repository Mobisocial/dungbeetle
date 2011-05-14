package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.database.sqlite.SQLiteOpenHelper;
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
import android.util.Log;

public class DBIdentityProvider implements IdentityProvider {

    public static final String TAG = "DBIdentityProvider";
	private final PublicKey mPubKey;
	private final String mPubKeyTag;
	private final PrivateKey mPrivKey;
	private final SQLiteOpenHelper mDb;
    private final String mName;

	public DBIdentityProvider(SQLiteOpenHelper db) {
        mDb = db;
		Cursor c = db.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();
        if(c.isAfterLast()){
            throw new IllegalStateException("Missing my_info entry!");
        }
        else{
            mPubKey = publicKeyFromString(c.getString(c.getColumnIndexOrThrow(MyInfo.PUBLIC_KEY)));
            mPrivKey = privateKeyFromString(c.getString(c.getColumnIndexOrThrow(MyInfo.PRIVATE_KEY)));
            mName = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
            mPubKeyTag = personIdForPublicKey(mPubKey);
        }
    }

	public String userName(){
        return mName;
    }

	public String userEmail(){
		Cursor c = mDb.getReadableDatabase().rawQuery("SELECT " + MyInfo.EMAIL + " FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();
        return c.getString(c.getColumnIndexOrThrow("email"));
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

	public Contact contactForUser(){
		Cursor c = mDb.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();
        long id = Contact.MY_ID;
        String name = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
        String email = c.getString(c.getColumnIndexOrThrow(MyInfo.EMAIL));
        Contact contact =  new Contact(id, mPubKeyTag, name, email, 0, 0, "");
        contact.picture = c.getBlob(c.getColumnIndexOrThrow(MyInfo.PICTURE)); 
        return contact;
    }

	public PublicKey publicKeyForPersonId(String id){
        Cursor c = mDb.getReadableDatabase().query(
            Contact.TABLE,
            new String[]{Contact.PUBLIC_KEY},
            Contact.PERSON_ID + " = ?",
            new String[]{id},
            null,null,null);
        c.moveToFirst();
        if(c.isAfterLast()){
            return null;
        }
        else{
            return publicKeyFromString(
                c.getString(c.getColumnIndexOrThrow(Contact.PUBLIC_KEY)));
        }
    }

	public List<PublicKey> publicKeysForContactIds(List<Long> ids){
        Iterator<Long> iter = ids.iterator();
        StringBuffer buffer = new StringBuffer();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if(iter.hasNext()){
                buffer.append(",");
            }
        }
        String idList = buffer.toString();
        Cursor c = mDb.getReadableDatabase().query(
            Contact.TABLE,
            new String[]{Contact.PUBLIC_KEY},
            Contact._ID + " IN (" + idList + ")",
            null,null,
            null,null);
        c.moveToFirst();
        ArrayList<PublicKey> result = new ArrayList<PublicKey>();
        while(!c.isAfterLast()){
            result.add(
                publicKeyFromString(
                    c.getString(
                        c.getColumnIndexOrThrow(
                            Contact.PUBLIC_KEY))));
            c.moveToNext();
        }
        return result;
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


    public static KeyPair generateKeyPair(){
        try {
            // Generate a 1024-bit Digital Signature Algorithm (RSA) key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            return keyGen.genKeyPair();        
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate key pair! " + e);
        }
    }


    public static String publicKeyToString(PublicKey pubkey){
        return Base64.encodeToString(pubkey.getEncoded(), false);
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


    public void close(){
        mDb.close();
    }


}
