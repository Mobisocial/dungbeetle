package edu.stanford.mobisocial.dungbeetle;
import java.util.Arrays;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import java.util.Date;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import java.security.KeyPair;
import java.security.PrivateKey;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
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
	public static final int VERSION = 16;
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
        db.execSQL("DROP TABLE IF EXISTS " + Subscriber.TABLE);
        db.execSQL("DROP TABLE IF EXISTS groups");
        db.execSQL("DROP TABLE IF EXISTS group_members");
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
                    Object.APP_ID, "TEXT",
                    Object.CONTACT_ID, "INTEGER",
                    Object.DESTINATION, "TEXT",
                    Object.JSON, "TEXT",
                    Object.TIMESTAMP, "INTEGER");
        createIndex(db, "INDEX", "objects_by_sequence_id", Object.TABLE, Object.SEQUENCE_ID);
        createIndex(db, "INDEX", "objects_by_feed_name", Object.TABLE, Object.FEED_NAME);
        createIndex(db, "INDEX", "objects_by_creator_id", Object.TABLE, Object.CONTACT_ID);

        createTable(db, Contact.TABLE,
                    Contact._ID, "INTEGER PRIMARY KEY",
                    Contact.NAME, "TEXT",
                    Contact.PUBLIC_KEY, "TEXT",
                    Contact.PERSON_ID, "TEXT",
                    Contact.EMAIL, "TEXT");
        createIndex(db, "UNIQUE INDEX", "contacts_by_person_id", Contact.TABLE, Contact.PERSON_ID);


		createTable(db, Subscriber.TABLE,
                    Subscriber._ID, "INTEGER PRIMARY KEY",
                    Subscriber.CONTACT_ID, "INTEGER",
                    Subscriber.FEED_NAME, "TEXT");
        createIndex(db, "UNIQUE INDEX", "subscribers_by_contact_id", Subscriber.TABLE, 
                    Subscriber.CONTACT_ID);


        createTable(db, "groups",
        			Group._ID, "INTEGER PRIMARY KEY",
        			Group.GROUP_ID, "TEXT",
        			Group.FEED_NAME, "TEXT");
        createIndex(db, "UNIQUE INDEX", "groups_by_group_id", "groups", "group_id");

        
        createTable(db, "group_members",
        			"_id", "INTEGER PRIMARY KEY",
        			"group_id", "TEXT",
        			"person_id", "TEXT");

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

    void setMyEmail(String email) {
        ContentValues cv = new ContentValues();
        cv.put("email", email);
        getWritableDatabase().update("my_info", cv, null, null);
    }

    long addToOutgoing(String appId, String to, String type, JSONObject json) {
        try{
            long timestamp = new Date().getTime();
            json.put("type", type);
            json.put("feedName", "direct");
            json.put("timestamp", timestamp);
            json.put("appId", appId);
            ContentValues cv = new ContentValues();
            cv.put(Object.APP_ID, appId);
            cv.put(Object.FEED_NAME, "direct");
            cv.put(Object.CONTACT_ID, Contact.MY_ID);
            cv.put(Object.DESTINATION, to);
            cv.put(Object.TYPE, type);
            cv.put(Object.JSON, json.toString());
            cv.put(Object.SEQUENCE_ID, 0);
            cv.put(Object.TIMESTAMP, timestamp);
            getWritableDatabase().insertOrThrow(Object.TABLE, null, cv);
            return 0;
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }

    long addToFeed(String appId, String feedName, String type, JSONObject json) {
        try{
            long nextSeqId = getFeedMaxSequenceId(Contact.MY_ID, feedName) + 1;
            long timestamp = new Date().getTime();
            json.put("type", type);
            json.put("feedName", feedName);
            json.put("sequenceId", nextSeqId);
            json.put("timestamp", timestamp);
            json.put("appId", appId);
            ContentValues cv = new ContentValues();
            cv.put(Object.APP_ID, appId);
            cv.put(Object.FEED_NAME, feedName);
            cv.put(Object.CONTACT_ID, Contact.MY_ID);
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


    long addObjectByJson(long contactId, JSONObject json) {
        try{
            long seqId = json.optLong("sequenceId");
            long timestamp = json.getLong("timestamp");
            String feedName = json.getString("feedName");
            String type = json.getString("type");
            String appId = json.getString("appId");
            ContentValues cv = new ContentValues();
            cv.put(Object.APP_ID, appId);
            cv.put(Object.FEED_NAME, feedName);
            cv.put(Object.CONTACT_ID, contactId);
            cv.put(Object.TYPE, type);
            cv.put(Object.SEQUENCE_ID, seqId);
            cv.put(Object.JSON, json.toString());
            cv.put(Object.TIMESTAMP, timestamp);
            getWritableDatabase().insertOrThrow("objects", null, cv);
            return seqId;
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

    long insertSubscriber(ContentValues cv) {
        try{
            String feedName = cv.getAsString(Subscriber.FEED_NAME);
            validate(feedName);
            return getWritableDatabase().insertOrThrow(Subscriber.TABLE, null, cv);
        }
        catch(Exception e){
            e.printStackTrace(System.err);
            return -1;
        }
    }
    
    long insertGroup(ContentValues cv) {
    	try{
    		String groupId = cv.getAsString("group_id");
    		validate(groupId);
    		String feedName = cv.getAsString("feed_name");
    		validate(feedName);
    		return getWritableDatabase().insertOrThrow("groups", null, cv);
    	}
    	catch(Exception e){
    		e.printStackTrace(System.err);
    		return -1;
    	}
    }
    
    long insertGroupMember(ContentValues cv) {
    	try{
    		String groupId = cv.getAsString("group_id");
    		validate(groupId);
    		String personId = cv.getAsString("person_id");
    		validate(personId);
    		return getWritableDatabase().insertOrThrow("group_members", null, cv);
    	}
    	catch(Exception e){
    		e.printStackTrace(System.err);
    		return -1;
    	}
    }

    private void validate(String val){
        assert (val != null) && val.length() > 0;
    }

    private long getFeedMaxSequenceId(long contactId, String feedName){
        Cursor c = getReadableDatabase().query(
            Object.TABLE,
            new String[]{ "max(" + Object.SEQUENCE_ID + ")" },
            Object.CONTACT_ID + "=? AND " + Object.FEED_NAME + "=?",
            new String[]{ String.valueOf(contactId), feedName },
            null,
            null,
            null);
        c.moveToFirst();
        if(!c.isAfterLast()){
            long max = c.getLong(0);
            Log.i(TAG, "Found max seq num: " + max);
            return max;
        }
        return -1;
    }


    public Cursor queryFeed(String appId, 
                            String feedName,
                            String[] projection, String selection,
                            String[] selectionArgs, String sortOrder
                            ){
        String select = andClauses(selection, Object.FEED_NAME + "='" + feedName + "'");
        select = andClauses(select, Object.APP_ID + "='" + appId + "'");
        return getReadableDatabase().query("objects", projection, 
                                           select, selectionArgs, 
                                           null, null, sortOrder, null);
    }

    public Cursor queryFeedLatest(String appId,
                                  String feedName, 
                                  String[] proj, String selection,
                                  String[] selectionArgs, String sortOrder){
        String select = andClauses(selection, Object.FEED_NAME + "='" + feedName + "'");
        select = andClauses(select, Object.APP_ID + "='" + appId + "'");

        // Don't allow custom projection. Just grab everything.
        String[] projection = new String[]{
            "o." + Object._ID + " as " + Object._ID,
            "o." + Object.TYPE + " as " + Object.TYPE,
            "o." + Object.SEQUENCE_ID + " as " + Object.SEQUENCE_ID,
            "o." + Object.FEED_NAME + " as " + Object.FEED_NAME,
            "o." + Object.CONTACT_ID + " as " + Object.CONTACT_ID,
            "o." + Object.DESTINATION + " as " + Object.DESTINATION,
            "o." + Object.JSON + " as " + Object.JSON,
            "o." + Object.TIMESTAMP + " as " + Object.TIMESTAMP,
            "o." + Object.APP_ID + " as " + Object.APP_ID
        };

        // Double this because select appears twice in full query
        String[] selectArgs = selectionArgs == null ? 
            new String[]{} : concat(selectionArgs, selectionArgs);
        String orderBy = sortOrder == null ? "" : " ORDER BY " + sortOrder;
        String q = joinWithSpaces("SELECT",projToStr(projection),
                                  "FROM (SELECT ", Object.CONTACT_ID, ",",
                                  "max(",Object.SEQUENCE_ID,")", "as max_seq_id", 
                                  "FROM", Object.TABLE,"WHERE",select,"GROUP BY",
                                  Object.CONTACT_ID,") AS x INNER JOIN ",
                                  "(SELECT * FROM ",Object.TABLE,
                                  "WHERE", select,") AS o ON ",
                                  "o.",Object.CONTACT_ID,"=", 
                                  "x.",Object.CONTACT_ID,"AND",
                                  "o.",Object.SEQUENCE_ID,"=x.max_seq_id",
                                  orderBy);
        Log.i(TAG, q);
        return getReadableDatabase().rawQuery(q,selectArgs);
    }


    public Cursor querySubscribers(String feedName) {
        return getReadableDatabase().query(
            Subscriber.TABLE,
            new String[]{ Subscriber._ID, Subscriber.CONTACT_ID },
            Subscriber.FEED_NAME + "=?",
            new String[]{ feedName },
            null,
            null,
            null,
            null);
    }

    public Cursor queryRecentlyAdded() {
        return getReadableDatabase().query(
            Object.TABLE,
            new String[]{ Object._ID, Object.JSON, 
                          Object.DESTINATION, Object.FEED_NAME },
            Object.CONTACT_ID + "=?",
            new String[]{ String.valueOf(Contact.MY_ID) },
            null,
            null,
            "timestamp DESC",
            "1");
    }

    public static String joinWithSpaces(String... strings) {
        return Util.join(Arrays.asList(strings), " ");
    }

    public static String projToStr(String[] strings) {
        if(strings == null) return "*";
        return Util.join(Arrays.asList(strings), ",");
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
