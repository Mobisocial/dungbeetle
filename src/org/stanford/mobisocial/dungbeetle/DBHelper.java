package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	static final int VERSION = 1;
	static final String TAG = "DBHelper";
	private PublicKey mPubKey;
	private String mPubKeyTag;
	private PrivateKey mPrivKey;

	public DBHelper(Context context) {
		super(
		    context, 
		    "DUNG_HEAP", 
		    new SQLiteDatabase.CursorFactory() {
		    	@Override
		    	public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
		    		return new SQLiteCursor(db, masterQuery, editTable, query);
		    	}
		    }, 
		    VERSION);
	}

    public PublicKey getPublicKey(){
        assert mPubKey != null;
        return mPubKey;
    }

    public PrivateKey getPrivateKey(){
        assert mPrivKey != null;
        return mPrivKey;
    }

    public String getMyCreatorTag(){
        assert mPubKeyTag != null;
        return mPubKeyTag;
    }

    public static String personIdForPublicKey(PublicKey key){
		String me = null;
		try {
			me = Util.SHA1(key.getEncoded());
		} catch (Exception e) {
			throw new IllegalArgumentException(
                "Could not compute SHA1 of public key.");
		}
		return me.substring(0, 10);
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

	@Override
	public void onOpen(SQLiteDatabase db) {
        mPubKey = getMyPubKey(db);
        mPrivKey = getMyPrivKey(db);
        mPubKeyTag = personIdForPublicKey(mPubKey);
	}

	private static PublicKey getMyPubKey(SQLiteDatabase db) {
		Cursor c = db.rawQuery("SELECT public_key FROM my_info", new String[] {});
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

	private static PrivateKey getMyPrivKey(SQLiteDatabase db) {
		Cursor c = db.rawQuery("SELECT private_key FROM my_info", new String[] {});
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


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();

		db.execSQL(
			"CREATE TABLE my_info (" +
            "public_key TEXT," +
            "private_key TEXT" +
			")");


		db.execSQL(
			"CREATE TABLE objects (" +
            "type TEXT," +
            "sequence_id INTEGER," +
            "feed_name TEXT," +
            "person_id TEXT," +
            "json TEXT" +
			")");
        db.execSQL("CREATE INDEX objects_by_sequence_id ON objects (sequence_id)");
        db.execSQL("CREATE INDEX objects_by_feed_name ON objects (feed_name)");
        db.execSQL("CREATE INDEX objects_by_person_id ON objects (person_id)");
        db.execSQL("CREATE INDEX objects_by_type ON objects (type)");


		db.execSQL(
			"CREATE TABLE contacts (" +
            "name TEXT," +
            "public_key TEXT," +
            "person_id TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX contacts_by_person_id ON contacts (person_id)");

		db.execSQL(
			"CREATE TABLE subscribers (" +
            "person_id TEXT," +
            "feed_name TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX subscribers_by_person_id ON subscribers (person_id)");

		db.execSQL(
			"CREATE TABLE subscriptions (" +
            "person_id TEXT," +
            "feed_name TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX subscriptions_by_person_id ON subscribers (person_id)");

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
            db.insertOrThrow("my_info", null, cv);

            Log.d(TAG, "Generated public key: " + pubKeyStr);
            Log.d(TAG, "Generated priv key: " + privKeyStr);

        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate key pair! " + e);
        }

        db.setVersion(VERSION);
        db.setTransactionSuccessful();
        db.endTransaction();
        this.onOpen(db);
	}


	long addToFeed(String personId, String feedName, String type, JSONObject json) {
        try{
            long maxSeqId = getFeedMaxSequenceId(personId, feedName);
            json.put("type", type);
            json.put("sequenceId", maxSeqId);
            ContentValues cv = new ContentValues();
            cv.put("feed_name", feedName);
            cv.put("person_id", personId);
            cv.put("type", type);
            cv.put("sequence_id", maxSeqId + 1);
            cv.put("json", json.toString());
            getWritableDatabase().insertOrThrow("objects", null, cv);
            return maxSeqId;
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

	long insertContact(ContentValues cv) {
        try{
            String pubKeyStr = cv.getAsString("public_key");
            assert (pubKeyStr != null) && pubKeyStr.length() > 0;
            PublicKey key = publicKeyFromString(pubKeyStr);
            String tag = personIdForPublicKey(key);
            cv.put("person_id", tag);
            String name = cv.getAsString("name");
            assert (name != null) && name.length() > 0;
            return getWritableDatabase().insertOrThrow("contacts", null, cv);
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

	long insertSubscription(ContentValues cv) {
        try{
            String personId = cv.getAsString("person_id");
            validate(personId);
            String feedName = cv.getAsString("feed_name");
            validate(feedName);
            return getWritableDatabase().insertOrThrow("subscriptions", null, cv);
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

	long insertSubscriber(ContentValues cv) {
        try{
            String personId = cv.getAsString("person_id");
            validate(personId);
            String feedName = cv.getAsString("feed_name");
            validate(feedName);
            return getWritableDatabase().insertOrThrow("subscribers", null, cv);
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

    private void validate(String val){
        assert (val != null) && val.length() > 0;
    }

    private long getFeedMaxSequenceId(String personId, String feedName){
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT max(sequence_id) FROM objects WHERE person_id = ? AND " + 
            " feed_name = ?",
            new String[] {personId, feedName});
        c.moveToFirst();
        if(c.isAfterLast()){
            return -1;
        }
        else{
            return c.getLong(0);
        }
    }

	public Cursor queryFeedLatest(String personId, 
                                  String feedName, 
                                  String objectType) {
		return getReadableDatabase().rawQuery(
            " SELECT json FROM objects WHERE " + 
            " person_id = :tag AND feed_name = :feed AND type = :type AND " + 
            " sequence_id = (SELECT max(sequence_id) FROM " + 
            " objects WHERE person_id = :tag AND feed_name = :feed AND type = :type)",
            new String[] {personId, feedName, objectType});
	}

	public Cursor queryFeedLatest(String feedName, 
                                  String objectType) {
        return getReadableDatabase().rawQuery(
            " SELECT json FROM " + 
            " (SELECT person_id,max(sequence_id) as max_seq_id FROM objects " + 
            " WHERE feed_name = :feed AND type = :type " + 
            " GROUP BY person_id) AS x INNER JOIN " + 
            " (SELECT * FROM objects " + 
            "  WHERE feed_name = :feed AND type = :type)  AS o ON " + 
            "  o.person_id = x.person_id AND o.sequence_id = x.max_seq_id",
            new String[] {feedName, objectType});
	}

	public Cursor queryAll(String personId, 
                           String feedName, 
                           String objectType) {
		return getReadableDatabase().rawQuery(
            " SELECT json FROM objects WHERE  " + 
            " person_id = ? AND feed_name = ? AND type = ?",
            new String[] {personId, feedName, objectType});
	}

}
