package edu.stanford.mobisocial.dungbeetle;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Object;
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

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = "DBHelper";
	public static final String DB_NAME = "DUNG_HEAP";
	public static final int VERSION = 1;

	public DBHelper(Context context) {
		super(
		    context, 
		    DB_NAME, 
		    new SQLiteDatabase.CursorFactory() {
		    	@Override
		    	public Cursor newCursor(SQLiteDatabase db, 
                                        SQLiteCursorDriver masterQuery, 
                                        String editTable, 
                                        SQLiteQuery query) {
		    		return new SQLiteCursor(db, masterQuery, editTable, query);
		    	}
		    }, 
		    VERSION);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
        // enable locking so we can safely share 
        // this instance around
        db.setLockingEnabled(true);
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
              + newVersion + ", which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS my_info");
        db.execSQL("DROP TABLE IF EXISTS objects");
        db.execSQL("DROP TABLE IF EXISTS contacts");
        db.execSQL("DROP TABLE IF EXISTS subscribers");
        db.execSQL("DROP TABLE IF EXISTS subscriptions");
        onCreate(db);
    }

    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();

		db.execSQL(
			"CREATE TABLE my_info (" +
            "public_key TEXT," +
            "private_key TEXT" +
			")");


		db.execSQL(
			"CREATE TABLE " + Object.TABLE + " (" +
            Object._ID + " INTEGER PRIMARY KEY, " +
            Object.TYPE + " TEXT," +
            Object.SEQUENCE_ID + " INTEGER," +
            Object.FEED_NAME + " TEXT," +
            Object.PERSON_ID + " TEXT," +
            Object.JSON + " TEXT" +
			")");
        db.execSQL("CREATE INDEX objects_by_sequence_id ON " + Object.TABLE + " (" + Object.SEQUENCE_ID + ")");
        db.execSQL("CREATE INDEX objects_by_feed_name ON " + Object.TABLE + " (" + Object.FEED_NAME + ")");
        db.execSQL("CREATE INDEX objects_by_person_id ON " + Object.TABLE + " (" + Object.PERSON_ID + ")");


		db.execSQL(
			"CREATE TABLE " + Contact.TABLE + " (" +
            Contact._ID + " INTEGER PRIMARY KEY, " +
            Contact.NAME + " TEXT," +
            Contact.PUBLIC_KEY + " TEXT," +
            Contact.PERSON_ID + " TEXT" +
			")");
        db.execSQL("CREATE UNIQUE INDEX contacts_by_person_id ON " + 
                   Contact.TABLE + " (" + Contact.PERSON_ID + ")");



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

        DBIdentityProvider.generateAndStoreKeys(db);
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
            PublicKey key = DBIdentityProvider.publicKeyFromString(pubKeyStr);
            String tag = DBIdentityProvider.makePersonIdForPublicKey(key);
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
            " SELECT _id,json FROM objects WHERE " + 
            " person_id = :tag AND feed_name = :feed AND type = :type AND " + 
            " sequence_id = (SELECT max(sequence_id) FROM " + 
            " objects WHERE person_id = :tag AND feed_name = :feed AND type = :type)",
            new String[] {personId, feedName, objectType});
	}

	public Cursor queryFeedLatest(String feedName, 
                                  String objectType) {
        return getReadableDatabase().rawQuery(
            " SELECT _id,json FROM " + 
            " (SELECT person_id,max(sequence_id) as max_seq_id FROM objects " + 
            " WHERE feed_name = :feed AND type = :type " + 
            " GROUP BY person_id) AS x INNER JOIN " + 
            " (SELECT * FROM objects " + 
            "  WHERE feed_name = :feed AND type = :type)  AS o ON " + 
            "  o.person_id = x.person_id AND o.sequence_id = x.max_seq_id ORDER BY _id",
            new String[] {feedName, objectType});
	}

	public Cursor queryFeedAll(String personId, 
                           String feedName) {
		return getReadableDatabase().rawQuery(
            " SELECT _id,json FROM objects WHERE  " + 
            " person_id = ? AND feed_name = ?",
            new String[] {personId, feedName});
	}

	public Cursor queryAll(String personId, 
                           String feedName, 
                           String objectType) {
		return getReadableDatabase().rawQuery(
            " SELECT _id,json FROM objects WHERE  " + 
            " person_id = ? AND feed_name = ? AND type = ?",
            new String[] {personId, feedName, objectType});
	}

}
