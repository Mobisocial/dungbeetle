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

import org.json.JSONException;
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

public class DataStore extends SQLiteOpenHelper {
	static final int VERSION = 1;
	static final String TAG = "DataStore";
	private PublicKey mPubKey;
	private String mPubKeyTag;
	private PrivateKey mPrivKey;

	public DataStore(Context context) {
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

    public String getMyPersonTag(){
        assert mPubKeyTag != null;
        return mPubKeyTag;
    }

    public static String personTagForPublicKey(PublicKey key){
		String me = null;
		try {
			me = Util.SHA1(key.getEncoded());
		} catch (Exception e) {
			throw new IllegalArgumentException(
                "Could not compute SHA1 of public key.");
		}
		return me.substring(0, 10);
    }

	@Override
	public void onOpen(SQLiteDatabase db) {
        mPubKey = getMyPubKey(db);
        mPrivKey = getMyPrivKey(db);
        mPubKeyTag = personTagForPublicKey(mPubKey);
	}

	private static PublicKey getMyPubKey(SQLiteDatabase db) {
		Cursor c = db.rawQuery("SELECT public_key FROM my_info", new String[] {});
		c.moveToFirst();
        if(c.isAfterLast()){
            throw new IllegalStateException("Missing my_info entry!");
        }
        else{
            try{
                String pubKeyStr = c.getString(0);
                Log.d(TAG, "Loaded public key: " + pubKeyStr);
                byte[] pubKeyBytes = Base64.decode(pubKeyStr);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
                return keyFactory.generatePublic(publicKeySpec);
            }
            catch(Exception e){
                throw new IllegalStateException("Error loading public key: " + e);
            }
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
                byte[] privKeyBytes = Base64.decode(privKeyStr);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
                return keyFactory.generatePrivate(privateKeySpec);
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

		db.execSQL("DROP_TABLE my_info");
		db.execSQL(
			"CREATE TABLE my_info (" +
            "public_key TEXT," +
            "private_key TEXT" +
			")");


		db.execSQL("DROP_TABLE objects");
		db.execSQL(
			"CREATE TABLE objects (" +
            "id INTEGER PRIMARY KEY," +
            "type TEXT," +
            "sequence_id INTEGER," +
            "feed_id INTEGER," +
            "json TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX objects_by_sequence_id ON objects (sequence_id)");
        db.execSQL("CREATE INDEX objects_by_feed_id ON objects (feed_id)");
        db.execSQL("CREATE INDEX objects_by_type ON objects (type)");


		db.execSQL("DROP_TABLE feeds");
		db.execSQL(
			"CREATE TABLE feeds (" +
            "id INTEGER PRIMARY KEY," +
            "person_tag TEXT," +
            "name TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX feeds_by_name ON feeds (name)");
        db.execSQL("CREATE INDEX objects_by_person_tag ON feeds (person_tag)");

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
            getWritableDatabase().insertOrThrow("my_info", null, cv);

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


	long addToFeed(String personTag, String feedName, String type, JSONObject json) {
        long feedId = getFeedId(personTag, feedName);
        if(feedId < 0){
            Log.e(TAG, "No feed named:" + feedName);
            return -1;
        }
        else{
            try{
                long maxSeqId = getFeedMaxSequenceId(feedId);
                json.put("type", type);
                json.put("sequenceId", maxSeqId);
                ContentValues cv = new ContentValues();
                cv.put("feed_id", feedId);
                cv.put("type", type);
                cv.put("sequence_id", maxSeqId + 1);
                cv.put("json", json.toString());
                getWritableDatabase().insert("objects", null, cv);
                return maxSeqId;
            }
            catch(Exception e){
                e.printStackTrace(System.err);
                return -1;
            }
        }
    }


    private long getFeedId(String personTag, String feedName){
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM feeds WHERE person_tag = ? AND name = ?", new String[] {personTag, feedName});
        c.moveToFirst();
        if(c.isAfterLast()){
            return -1;
        }
        else{
            return c.getLong(0);
        }
    }


    private long getFeedMaxSequenceId(long id){
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT max(sequence_id) FROM objects WHERE feed_id = ?", 
            new String[] {Long.toString(id)});
        c.moveToFirst();
        if(c.isAfterLast()){
            return -1;
        }
        else{
            return c.getLong(0);
        }
    }

	public JSONObject getLatestObjectOfType(String personTag, String feedName, String objectType) {
        long feedId = getFeedId(personTag, feedName);
		Cursor c = getReadableDatabase().rawQuery(
            "SELECT json FROM objects WHERE feed_id = ? AND " + 
            " sequence_id = SELECT max(sequence_id) FROM objects where type = ?",
            new String[] {Long.toString(feedId), objectType});
        if(c.isAfterLast()){
            return null;
        }
        else{
            try{
                return new JSONObject(c.getString(0));
            }
            catch(JSONException e){
                return null;
            }
        }
	}


}
