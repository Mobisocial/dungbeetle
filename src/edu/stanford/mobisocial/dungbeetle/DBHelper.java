package edu.stanford.mobisocial.dungbeetle;
import java.util.Date;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import java.security.KeyPair;
import java.security.PrivateKey;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.database.sqlite.SQLiteQueryBuilder;
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
	public static final int VERSION = 9;
    private final Context mContext;

	public DBHelper(Context context) {
		super(
		    context, 
		    DB_NAME, 
		    new SQLiteDatabase.CursorFactory() {
		    	@Override
		    	public Cursor newCursor(
                    SQLiteDatabase db, 
                    SQLiteCursorDriver masterQuery, 
                    String editTable, 
                    SQLiteQuery query) {
		    		return new SQLiteCursor(db, masterQuery, editTable, query);
		    	}
		    }, 
		    VERSION);
        mContext = context;
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
        db.execSQL("DROP TABLE IF EXISTS " + Object.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Contact.TABLE);
        db.execSQL("DROP TABLE IF EXISTS subscribers");
        db.execSQL("DROP TABLE IF EXISTS subscriptions");
        onCreate(db);
    }

    private void createTable(SQLiteDatabase db, String tableName, String... cols){
        assert cols.length % 2 == 0;
        String s = "CREATE TABLE " + tableName + " (";
        for(int i = 0; i < cols.length; i += 2){
            s += cols[i] + " " + cols[i + 1];
            if(i < (cols.length - 2)){
                s += ", ";
            }
            else{
                s += " ";
            }
        }
        s += ")";
        Log.i(TAG, s);
        db.execSQL(s);
    }

    private void createIndex(SQLiteDatabase db, String type, String name, String tableName, String col){
        String s = "CREATE " + type + " " + name + " on " + tableName + " (" + col + ")";
        Log.i(TAG, s);
        db.execSQL(s);
    } 

    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();

        createTable(db, "my_info", 
                    "public_key", "TEXT",
                    "private_key", "TEXT",
                    "name", "TEXT",
                    "email", "TEXT"
                    );


        createTable(db, Object.TABLE,
                    Object._ID, "INTEGER PRIMARY KEY",
                    Object.TYPE, "TEXT",
                    Object.SEQUENCE_ID, "INTEGER",
                    Object.FEED_NAME, "TEXT",
                    Object.PERSON_ID, "TEXT",
                    Object.JSON, "TEXT",
                    Object.TIMESTAMP, "INTEGER");
        createIndex(db, "INDEX", "objects_by_sequence_id", Object.TABLE, Object.SEQUENCE_ID);
        createIndex(db, "INDEX", "objects_by_feed_name", Object.TABLE, Object.FEED_NAME);
        createIndex(db, "INDEX", "objects_by_person_id", Object.TABLE, Object.PERSON_ID);


        createTable(db, Contact.TABLE,
                    Contact._ID, "INTEGER PRIMARY KEY",
                    Contact.NAME, "TEXT",
                    Contact.PUBLIC_KEY, "TEXT",
                    Contact.PERSON_ID, "TEXT",
                    Contact.EMAIL, "TEXT");
        createIndex(db, "UNIQUE INDEX", "contacts_by_person_id", Contact.TABLE, Contact.PERSON_ID);


		createTable(db, "subscribers",
                    "_id", "INTEGER PRIMARY KEY",
                    "person_id", "TEXT",
                    "feed_name", "TEXT");
        createIndex(db, "UNIQUE INDEX", "subscribers_by_person_id", "subscribers", "person_id");


		createTable(db, "subscriptions",
                    "_id", "INTEGER PRIMARY KEY",
                    "person_id", "TEXT",
                    "feed_name", "TEXT");
        createIndex(db, "UNIQUE INDEX", "subscriptions_by_person_id", "subscriptions", "person_id");


        generateAndStorePersonalInfo(db);

        db.setVersion(VERSION);
        db.setTransactionSuccessful();
        db.endTransaction();
        this.onOpen(db);
	}

    private void generateAndStorePersonalInfo(SQLiteDatabase db){
        String email = getUserEmail();
        String name = email; // How to get this?

        KeyPair keypair = DBIdentityProvider.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();
        String pubKeyStr = Base64.encodeToString(publicKey.getEncoded(), false);
        String privKeyStr = Base64.encodeToString(privateKey.getEncoded(), false);
        ContentValues cv = new ContentValues();
        cv.put("public_key", pubKeyStr);
        cv.put("private_key", privKeyStr);
        cv.put("name", name);
        cv.put("email", email);
        db.insertOrThrow("my_info", null, cv);
        Log.d(TAG, "Generated public key: " + pubKeyStr);
        Log.d(TAG, "Generated priv key: " + privKeyStr);
    }

    private String getUserEmail(){
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        String possibleEmail = "NA";
        for (Account account : accounts) {
            if(account.name.length() > 0){
                possibleEmail = account.name;
            }
        }
        return possibleEmail;
    }

    long addToFeed(String personId, String feedName, String type, JSONObject json) {
        try{
            long nextSeqId = getFeedMaxSequenceId(personId, feedName) + 1;
            long timestamp = new Date().getTime();
            json.put("type", type);
            json.put("feedName", feedName);
            json.put("sequenceId", nextSeqId);
            json.put("timestamp", timestamp);
            ContentValues cv = new ContentValues();
            cv.put(Object.FEED_NAME, feedName);
            cv.put(Object.PERSON_ID, personId);
            cv.put(Object.TYPE, type);
            cv.put(Object.SEQUENCE_ID, nextSeqId);
            cv.put(Object.JSON, json.toString());
            cv.put(Object.TIMESTAMP, timestamp);
            getWritableDatabase().insertOrThrow("objects", null, cv);
            return nextSeqId;
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }


    long addExistingToFeed(String personId, JSONObject json) {
        try{
            long nextSeqId = json.getLong("sequenceId");
            long timestamp = json.getLong("timestamp");
            String feedName = json.getString("feedName");
            String type = json.getString("type");
            ContentValues cv = new ContentValues();
            cv.put(Object.FEED_NAME, feedName);
            cv.put(Object.PERSON_ID, personId);
            cv.put(Object.TYPE, type);
            cv.put(Object.SEQUENCE_ID, nextSeqId);
            cv.put(Object.JSON, json.toString());
            cv.put(Object.TIMESTAMP, timestamp);
            getWritableDatabase().insertOrThrow("objects", null, cv);
            return nextSeqId;
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

    long insertContact(ContentValues cv) {
        try{
            Log.i(TAG, "Inserting contact: " + cv);
            String pubKeyStr = cv.getAsString(Contact.PUBLIC_KEY);
            assert (pubKeyStr != null) && pubKeyStr.length() > 0;
            PublicKey key = DBIdentityProvider.publicKeyFromString(pubKeyStr);
            String tag = DBIdentityProvider.makePersonIdForPublicKey(key);
            cv.put(Contact.PERSON_ID, tag);
            String name = cv.getAsString(Contact.NAME);
            assert (name != null) && name.length() > 0;
            return getWritableDatabase().insertOrThrow(Contact.TABLE, null, cv);
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
            "SELECT max(sequence_id) FROM objects WHERE person_id = ? AND feed_name = ?",
            new String[] {personId, feedName});
        c.moveToFirst();
        if(!c.isAfterLast()){
            long max = c.getLong(0);
            Log.i(TAG, "Found max seq num: " + max);
            return max;
        }
        return -1;
    }


    public Cursor queryFeed(String feedName,
                            String[] projection, String selection,
                            String[] selectionArgs, String sortOrder
                            ){
        String select = andClauses(selection,"feed_name='" + feedName + "'");
        return getReadableDatabase().rawQuery(
            SQLiteQueryBuilder.buildQueryString(false, "objects", projection, select, null, null, sortOrder, null),
            selectionArgs);
    }

    public Cursor queryFeedLatest(String feedName, 
                                  String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder){
        String select = andClauses(selection,"feed_name='" + feedName + "'");
        // Double this because select appears twice in full query
        String[] selectArgs = selectionArgs == null ? 
            new String[]{} : concat(selectionArgs, selectionArgs);
        String orderBy = sortOrder == null ? "" : " ORDER BY " + sortOrder;
        String q = 
            " SELECT " + projToStr(projection) + " FROM " + 
            " (SELECT person_id,max(sequence_id) as max_seq_id FROM objects " + 
            " WHERE " + select + 
            " GROUP BY person_id) AS x INNER JOIN " + 
            " (SELECT * FROM objects " + 
            " WHERE " + select + ") AS o ON " + 
            " o.person_id = x.person_id AND o.sequence_id = x.max_seq_id " + orderBy;
        return getReadableDatabase().rawQuery(q,selectArgs);
    }


    public Cursor querySubscribers(String feedName) {
        return getReadableDatabase().rawQuery(
            " SELECT _id,person_id FROM subscribers WHERE feed_name = ?",
            new String[] {feedName});
    }

    public Cursor queryRecentlyAdded(String personId, String feedName) {
        return getReadableDatabase().rawQuery(
            " SELECT _id,json FROM objects WHERE " + 
            " person_id = ? AND feed_name = ? ORDER BY timestamp DESC LIMIT 1",
            new String[] { personId, feedName});
    }

    public static String projToStr(String[] strings) {
        if(strings == null) return "*";
        StringBuffer sb = new StringBuffer();
        for (int i=0; i < strings.length; i++) {
            if (i != 0) sb.append(",");
            sb.append(strings[i]);
        }
        return sb.toString();
    }

    public static String andClauses(String A, String B) {
        if(A == null && B == null) return "1 = 1";
        if(A == null) return B;
        if(B == null) return A;
        return A + " AND " + B;
    }

    public static String[] concat(String[] A, String[] B) {
        String[] C = new String[A.length + B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);
        return C;
    }

}
