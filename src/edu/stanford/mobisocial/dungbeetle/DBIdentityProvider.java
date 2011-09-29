package edu.stanford.mobisocial.dungbeetle;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.util.Base64;

public class DBIdentityProvider implements IdentityProvider {

    public static final String TAG = "DBIdentityProvider";
	private final RSAPublicKey mPubKey;
	private final String mPubKeyTag;
	private final RSAPrivateKey mPrivKey;
    private final String mEmail;
    private final String mName;
    private final DBHelper mHelper;

    private final String mPubKeyString;

    private Exception mUnclosedException;
    @Override
    protected void finalize() throws Throwable {
    	if(mUnclosedException != null) {
    		throw mUnclosedException;
    	}
        super.finalize();
    }

	public DBIdentityProvider(DBHelper helper) {
		mHelper = helper;
		helper.addRef();
		mUnclosedException = new Exception("Finalized without close being called. Created at...");
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();
        if (c.isAfterLast()) {
            c.close();
            throw new IllegalStateException("Missing my_info entry!");
        }

        mPubKeyString = c.getString(c.getColumnIndexOrThrow(MyInfo.PUBLIC_KEY));
        mPubKey = publicKeyFromString(mPubKeyString);
        mPrivKey = privateKeyFromString(c.getString(c.getColumnIndexOrThrow(MyInfo.PRIVATE_KEY)));
        mName = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
        mEmail = c.getString(c.getColumnIndexOrThrow(MyInfo.EMAIL));
        mPubKeyTag = personIdForPublicKey(mPubKey);

        Log.d(TAG, c.getCount() + " public keys");
        c.close();
    }

	public String userName() {
		return mName;
    }

	public String userEmail() {
	    return mEmail;
    }

    public String userProfile() {
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();

		JSONObject obj = new JSONObject();
        try {
            obj.put("name", c.getString(c.getColumnIndexOrThrow(MyInfo.NAME)));
            obj.put("picture", Base64.encodeToString(c.getBlob(c.getColumnIndexOrThrow(MyInfo.PICTURE)), false));
            
        } catch(JSONException e) { }
        c.close();
        return obj.toString(); 
    }

    public String userPublicKeyString() {
        return mPubKeyString;
    }

	public RSAPublicKey userPublicKey(){
        return mPubKey;
    }

	public RSAPrivateKey userPrivateKey(){
        return mPrivKey;
    }

	public String userPersonId(){
        return mPubKeyTag;
    }

	public Contact contactForUser(){
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		c.moveToFirst();
        long id = Contact.MY_ID;
        String name = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
        String email = c.getString(c.getColumnIndexOrThrow(MyInfo.EMAIL));
        Contact contact =  new Contact(id, mPubKeyTag, name, email, 0, 0, false, null, "");
        contact.picture = c.getBlob(c.getColumnIndexOrThrow(MyInfo.PICTURE)); 
        c.close();
        return contact;
    }

	public RSAPublicKey publicKeyForPersonId(String id){
		if(id.equals(mPubKeyTag)) {
			return mPubKey;
		}
        Cursor c = mHelper.getReadableDatabase().query(Contact.TABLE, new String[]{Contact.PUBLIC_KEY},
            Contact.PERSON_ID + " = ?", new String[]{id}, null, null, null);
        try {
	        c.moveToFirst();
	        if (c.isAfterLast()) {
	            return null;
	        }
	
	        RSAPublicKey k = (RSAPublicKey)publicKeyFromString(
	            c.getString(c.getColumnIndexOrThrow(Contact.PUBLIC_KEY)));
	        return k;
        } finally {
        	c.close();
        }
    }

	public List<RSAPublicKey> publicKeysForContactIds(List<Long> ids){
        ArrayList<RSAPublicKey> result = new ArrayList<RSAPublicKey>(ids.size());
        SQLiteStatement s = mHelper.getReadableDatabase().compileStatement("SELECT " + Contact.PUBLIC_KEY + " FROM " + Contact.TABLE + " WHERE " + Contact._ID + " = ?");
		for(Long id : ids) {
			s.bindLong(1, id.longValue());
			String pks = s.simpleQueryForString();
			result.add(publicKeyFromString(pks));
		}
		s.close();
		return result;
    }

    public String personIdForPublicKey(RSAPublicKey key){
        return edu.stanford.mobisocial.bumblebee.util.Util.makePersonIdForPublicKey(key);
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

    public static RSAPublicKey publicKeyFromString(String str){
        try{
            byte[] pubKeyBytes = Base64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            return (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);                
        }
        catch(Exception e){
            throw new IllegalStateException("Error loading public key: " + e);
        }
    }

    public static RSAPrivateKey privateKeyFromString(String str){
        try{
            byte[] privKeyBytes = Base64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
            return (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
        }
        catch(Exception e){
            throw new IllegalStateException("Error loading public key: " + e);
        }
    }

    @Override
    public void close() {
    	mUnclosedException = null;
    	mHelper.close();
    }
}
