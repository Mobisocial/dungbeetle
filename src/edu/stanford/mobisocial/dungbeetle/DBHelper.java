package edu.stanford.mobisocial.dungbeetle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.os.Environment;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.objects.SharedSecretObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = "DBHelper";
	public static final String DB_NAME = "MUSUBI.db";
	//for legacy purposes
	public static final String OLD_DB_NAME = "DUNG_HEAP.db";
	public static final String DB_PATH = "/data/edu.stanford.mobisocial.dungbeetle/databases/";
	public static final int VERSION = 35;
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
	
    public boolean importDatabase(String dbPath) throws IOException {

        // Close the SQLiteOpenHelper so it will commit the created empty
        // database to internal storage.
        close();
        
        File data = Environment.getDataDirectory();
        File newDb = new File(data, dbPath);
        File oldDb = new File(data, DB_PATH+DB_NAME);
        if (newDb.exists()) {
            Util.copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.
            getWritableDatabase().close();
            checkEncodedExists(getReadableDatabase());
                
            Intent DBServiceIntent = new Intent(mContext, DungBeetleService.class);
            mContext.stopService(DBServiceIntent);
            mContext.startService(DBServiceIntent);
            return true;
        }
        return false;
    }
	
    public boolean importDatabaseFromSD(String dbPath) throws IOException {

        // Close the SQLiteOpenHelper so it will commit the created empty
        // database to internal storage.
        close();
        
        File data = Environment.getDataDirectory();
        File newDb = new File(dbPath);
        File oldDb = new File(data, DB_PATH+DB_NAME);
        if (newDb.exists()) {
            Util.copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.
            getWritableDatabase().close();
            checkEncodedExists(getReadableDatabase());
                
            Intent DBServiceIntent = new Intent(mContext, DungBeetleService.class);
            mContext.stopService(DBServiceIntent);
            mContext.startService(DBServiceIntent);
            return true;
        }
        return false;
    }

    private void checkEncodedExists(SQLiteDatabase db) {
        	Cursor c = db.rawQuery("SELECT * FROM " + DbObject.TABLE, null);
        	try {
            	c.getColumnIndexOrThrow(DbObject.ENCODED);
        	}
        	catch(Exception e) {
            Log.w(TAG, "Adding column 'E' to object table.");
            db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.ENCODED + " BLOB");
            createIndex(db, "INDEX", "objects_by_encoded", DbObject.TABLE, DbObject.ENCODED);
        	}
    }

	@Override
	public void onOpen(SQLiteDatabase db) {
        // enable locking so we can safely share 
        // this instance around
        db.setLockingEnabled(true);
        checkEncodedExists(db);
        Log.w(TAG, "dbhelper onopen");
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if(oldVersion <= 23){
            Log.w(TAG, "Schema too old to migrate, dropping all."); 
            dropAll(db);
            onCreate(db);
            return;
        }

        if(oldVersion <= 24) {
            Log.w(TAG, "Adding columns 'presence' and 'status' to contact table.");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.STATUS + " TEXT");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.PRESENCE + " INTEGER DEFAULT " + Presence.AVAILABLE);
        }

        if(oldVersion <= 25) {
            Log.w(TAG, "Adding columns 'presence' and 'status' to contact table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.FEED_NAME + " TEXT");
        }

        if(oldVersion <= 26) {
            Log.w(TAG, "Adding column 'picture' to contact table.");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.PICTURE + " BLOB");
        }

        if(oldVersion <= 27) {
            Log.w(TAG, "Adding column 'last_presence_time' to contact table.");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.LAST_PRESENCE_TIME + " INTEGER DEFAULT 0");
        }

        if(oldVersion <= 28) {
            Log.w(TAG, "Adding column 'picture' to my_info table.");
            db.execSQL("ALTER TABLE " + MyInfo.TABLE + " ADD COLUMN " + MyInfo.PICTURE + " BLOB");
        }
        if(oldVersion <= 29) {
            Log.w(TAG, "Adding column 'version' to group table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.VERSION + " INTEGER DEFAULT -1");
        }
        if(oldVersion <= 30) {
            Log.w(TAG, "Adding column 'E' to object table.");
            db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.ENCODED + " BLOB");
            createIndex(db, "INDEX", "objects_by_encoded", DbObject.TABLE, DbObject.ENCODED);
        }
        if(oldVersion <= 31) {
            Log.w(TAG, "Adding column 'child_feed' to object table.");
            db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.CHILD_FEED_NAME + " TEXT");
            createIndex(db, "INDEX", "child_feeds", DbObject.TABLE, DbObject.CHILD_FEED_NAME);
        }
        if(oldVersion <= 32) {
            // Bug fix.
            Log.w(TAG, "Updating app state objects.");
            db.execSQL("UPDATE " + DbObject.TABLE + " SET " + DbObject.CHILD_FEED_NAME +
                    " = NULL WHERE " + DbObject.CHILD_FEED_NAME + " = " + DbObject.FEED_NAME);
        }
        if(oldVersion <= 33) {
            Log.w(TAG, "Adding column 'nearby' to contact table.");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.NEARBY + " INTEGER DEFAULT 0");
        }
        if(oldVersion <= 34) {
            Log.w(TAG, "Adding column 'secret' to contact table.");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.SHARED_SECRET + " BLOB");
            
        }
        db.setVersion(VERSION);
    }

    private void dropAll(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS " + MyInfo.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbObject.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Contact.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Subscriber.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Group.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + GroupMember.TABLE);
    }

    private void createTable(SQLiteDatabase db, String tableName, String[] uniqueCols, String... cols){
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
        if(uniqueCols != null && uniqueCols.length > 0){
            s+= ", UNIQUE (" + Util.join(uniqueCols, ",") + ")";
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

            createTable(db, MyInfo.TABLE, null,
                        MyInfo._ID, "INTEGER PRIMARY KEY",
                        MyInfo.PUBLIC_KEY, "TEXT",
                        MyInfo.PRIVATE_KEY, "TEXT",
                        MyInfo.NAME, "TEXT",
                        MyInfo.EMAIL, "TEXT",
                        MyInfo.PICTURE, "BLOB"
                        );

            createTable(db, DbObject.TABLE, null,
                        DbObject._ID, "INTEGER PRIMARY KEY",
                        DbObject.TYPE, "TEXT",
                        DbObject.SEQUENCE_ID, "INTEGER",
                        DbObject.FEED_NAME, "TEXT",
                        DbObject.APP_ID, "TEXT",
                        DbObject.CONTACT_ID, "INTEGER",
                        DbObject.DESTINATION, "TEXT",
                        DbObject.JSON, "TEXT",
                        DbObject.TIMESTAMP, "INTEGER",
                        DbObject.SENT, "INTEGER DEFAULT 0",
                        DbObject.ENCODED, "BLOB",
                        DbObject.CHILD_FEED_NAME, "TEXT"
                        );
            createIndex(db, "INDEX", "objects_by_sequence_id", DbObject.TABLE, DbObject.SEQUENCE_ID);
            createIndex(db, "INDEX", "objects_by_feed_name", DbObject.TABLE, DbObject.FEED_NAME);
            createIndex(db, "INDEX", "objects_by_creator_id", DbObject.TABLE, DbObject.CONTACT_ID);
            createIndex(db, "INDEX", "objects_by_encoded", DbObject.TABLE, DbObject.ENCODED);
            createIndex(db, "INDEX", "child_feeds", DbObject.TABLE, DbObject.CHILD_FEED_NAME);

            createTable(db, Contact.TABLE, null,
                        Contact._ID, "INTEGER PRIMARY KEY",
                        Contact.NAME, "TEXT",
                        Contact.PUBLIC_KEY, "TEXT",
                        Contact.SHARED_SECRET, "BLOB",
                        Contact.PERSON_ID, "TEXT",
                        Contact.EMAIL, "TEXT",
                        Contact.PRESENCE, "INTEGER DEFAULT " + Presence.AVAILABLE,
                        Contact.LAST_PRESENCE_TIME, "INTEGER DEFAULT 0",
                        Contact.NEARBY, "INTEGER DEFAULT 0",
                        Contact.STATUS, "TEXT",
                        Contact.PICTURE, "BLOB");
            createIndex(db, "UNIQUE INDEX", "contacts_by_person_id", Contact.TABLE, Contact.PERSON_ID);


		    createTable(db, Subscriber.TABLE, new String[]{Subscriber.CONTACT_ID, Subscriber.FEED_NAME},
                        Subscriber._ID, "INTEGER PRIMARY KEY",
                        Subscriber.CONTACT_ID, "INTEGER REFERENCES " + Contact.TABLE + "(" + Contact._ID + ") ON DELETE CASCADE",
                        Subscriber.FEED_NAME, "TEXT");
            createIndex(db, "INDEX", "subscribers_by_contact_id", Subscriber.TABLE, Subscriber.CONTACT_ID);

            createTable(db, Group.TABLE, null,
            			Group._ID, "INTEGER PRIMARY KEY",
            			Group.NAME, "TEXT",
                    Group.FEED_NAME, "TEXT",
                    Group.DYN_UPDATE_URI, "TEXT",
                    Group.VERSION, "INTEGER DEFAULT -1"
                        );
            
            createTable(db, GroupMember.TABLE, null,
            			GroupMember._ID, "INTEGER PRIMARY KEY",
            			GroupMember.GROUP_ID, "INTEGER REFERENCES " + Group.TABLE + "(" + Group._ID + ") ON DELETE CASCADE",
            			GroupMember.CONTACT_ID, "INTEGER REFERENCES " + Contact.TABLE + "(" + Contact._ID + ") ON DELETE CASCADE",
                        GroupMember.GLOBAL_CONTACT_ID, "TEXT");
            createIndex(db, "UNIQUE INDEX", "group_members_by_group_id", GroupMember.TABLE, 
                        GroupMember.GROUP_ID + "," + GroupMember.CONTACT_ID);


            generateAndStorePersonalInfo(db);

            db.setVersion(VERSION);
            db.setTransactionSuccessful();
            db.endTransaction();
            this.onOpen(db);
        //}
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
        cv.put(MyInfo.PUBLIC_KEY, pubKeyStr);
        cv.put(MyInfo.PRIVATE_KEY, privKeyStr);
        cv.put(MyInfo.NAME, name);
        cv.put(MyInfo.EMAIL, email);
        db.insertOrThrow(MyInfo.TABLE, null, cv);
        Log.d(TAG, "Generated public key: " + pubKeyStr);
        Log.d(TAG, "Generated priv key: **************");
    }

    public void generateAndStorePersonalInfo(){
        SQLiteDatabase db = getWritableDatabase();

        String email = getUserEmail();
        String name = email; // How to get this?

        KeyPair keypair = DBIdentityProvider.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();
        String pubKeyStr = Base64.encodeToString(publicKey.getEncoded(), false);
        String privKeyStr = Base64.encodeToString(privateKey.getEncoded(), false);
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.PUBLIC_KEY, pubKeyStr);
        cv.put(MyInfo.PRIVATE_KEY, privKeyStr);
        cv.put(MyInfo.NAME, name);
        cv.put(MyInfo.EMAIL, email);
        db.insertOrThrow(MyInfo.TABLE, null, cv);
        Log.d(TAG, "Generated public key: " + pubKeyStr);
        Log.d(TAG, "Generated priv key: **************");
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

    void setMyName(String name) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.NAME, name);
        getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }

    void setMyEmail(String email) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.EMAIL, email);
        getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }

    long addToOutgoing(String appId, String to, String type, JSONObject json){
        return addToOutgoing(getWritableDatabase(), 
                             appId, to, type, json);
    }
    
    long addToOutgoing(
        SQLiteDatabase db, String appId, String to, String type, JSONObject json) {
        try{
            long timestamp = new Date().getTime();
            json.put("type", type);
            json.put("feedName", "direct");
            json.put("timestamp", timestamp);
            json.put("appId", appId);
            ContentValues cv = new ContentValues();
            cv.put(DbObject.APP_ID, appId);
            cv.put(DbObject.FEED_NAME, "direct");
            cv.put(DbObject.CONTACT_ID, Contact.MY_ID);
            cv.put(DbObject.DESTINATION, to);
            cv.put(DbObject.TYPE, type);
            cv.put(DbObject.JSON, json.toString());
            cv.put(DbObject.SEQUENCE_ID, 0);
            cv.put(DbObject.TIMESTAMP, timestamp);
            db.insertOrThrow(DbObject.TABLE, null, cv);
            return 0;
        }
        catch(Exception e){
            // TODO, too spammy
            //e.printStackTrace(System.err);
            return -1;
        }
    }

    long addToFeed(String appId, String feedName, String type, JSONObject json) {
        try{
            long nextSeqId = getFeedMaxSequenceId(Contact.MY_ID, feedName) + 1;
            long timestamp = new Date().getTime();
            json.put(DbObjects.TYPE, type);
            json.put(DbObjects.FEED_NAME, feedName);
            json.put(DbObjects.SEQUENCE_ID, nextSeqId);
            json.put(DbObjects.TIMESTAMP, timestamp);
            json.put(DbObjects.APP_ID, appId);

            ContentValues cv = new ContentValues();
            cv.put(DbObject.APP_ID, appId);
            cv.put(DbObject.FEED_NAME, feedName);
            cv.put(DbObject.CONTACT_ID, Contact.MY_ID);
            cv.put(DbObject.TYPE, type);
            cv.put(DbObject.SEQUENCE_ID, nextSeqId);
            cv.put(DbObject.JSON, json.toString());
            cv.put(DbObject.TIMESTAMP, timestamp);
            if (json.has(DbObject.CHILD_FEED_NAME)) {
                cv.put(DbObject.CHILD_FEED_NAME, json.optString(DbObject.CHILD_FEED_NAME));
            }
            getWritableDatabase().insertOrThrow(DbObject.TABLE, null, cv);
            return nextSeqId;
        }
        catch(Exception e) {
            // TODO, too spammy
            //e.printStackTrace(System.err);
            return -1;
        }
    }


    long addObjectByJson(long contactId, JSONObject json, byte[] encoded){
        try{
            long seqId = json.optLong(DbObjects.SEQUENCE_ID);
            long timestamp = json.getLong(DbObjects.TIMESTAMP);
            String feedName = json.getString(DbObjects.FEED_NAME);
            String type = json.getString(DbObjects.TYPE);
            String appId = json.getString(DbObjects.APP_ID);
            ContentValues cv = new ContentValues();
            cv.put(DbObject.APP_ID, appId);
            cv.put(DbObject.FEED_NAME, feedName);
            cv.put(DbObject.CONTACT_ID, contactId);
            cv.put(DbObject.TYPE, type);
            cv.put(DbObject.SEQUENCE_ID, seqId);
            cv.put(DbObject.JSON, json.toString());
            cv.put(DbObject.TIMESTAMP, timestamp);
            cv.put(DbObject.ENCODED, encoded);
            cv.put(DbObject.SENT, 1);
            if (json.has(DbObject.CHILD_FEED_NAME)) {
                cv.put(DbObject.CHILD_FEED_NAME, json.optString(DbObject.CHILD_FEED_NAME));
            }
            getWritableDatabase().insertOrThrow(DbObject.TABLE, null, cv);

            ContentResolver resolver = mContext.getContentResolver();
            Cursor c = getFeedDependencies(feedName);
            while (c.moveToNext()) {
                resolver.notifyChange(Feed.uriForName(c.getString(0)), null);
            }

            return seqId;
        }
        catch(Exception e){
            Log.e(TAG, e.getMessage());
            return -1;
        }
    }


    long insertContact(ContentValues cv) {
        return insertContact(getWritableDatabase(), cv);
    }

    long insertContact(SQLiteDatabase db, ContentValues cv) {
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
            Log.e(TAG, e.getMessage());
            return -1;
        }
    }

    long insertSubscriber(ContentValues cv) {
        return insertSubscriber(getWritableDatabase(), cv);
    }

    long insertSubscriber(SQLiteDatabase db, ContentValues cv) {
        try{
            String feedName = cv.getAsString(Subscriber.FEED_NAME);
            validate(feedName);
            return db.insertOrThrow(Subscriber.TABLE, null, cv);
        }
        catch(Exception e){
            Log.e(TAG, e.getMessage());
            return -1;
        }
    }


    long insertGroup(ContentValues cv) {
        return insertGroup(getWritableDatabase(), cv);
    }

    long insertGroup(SQLiteDatabase db, ContentValues cv) {
    	try{
    		validate(cv.getAsString(Group.NAME));
    		return db.insertOrThrow(Group.TABLE, null, cv);
    	}
    	catch(Exception e){
            Log.e(TAG, e.getMessage());
    		return -1;
    	}
    }
    
    long insertGroupMember(ContentValues cv) {
        return insertGroupMember(getWritableDatabase(), cv);
    }

    long insertGroupMember(SQLiteDatabase db, ContentValues cv) {
    	try{
    		return db.insertOrThrow(GroupMember.TABLE, null, cv);
    	}
    	catch(Exception e){
            // TODO, too spammy
            //e.printStackTrace(System.err);
    		return -1;
    	}
    }
    
    private String validate(String val){
        assert (val != null) && val.length() > 0;
        return val;
    }

    private long getFeedMaxSequenceId(long contactId, String feedName){
        Cursor c = getReadableDatabase().query(
            DbObject.TABLE,
            new String[]{ "max(" + DbObject.SEQUENCE_ID + ")" },
            DbObject.CONTACT_ID + "=? AND " + DbObject.FEED_NAME + "=?",
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

    public Cursor queryFeedList(String[] projection, String selection, String[] selectionArgs,
            String sortOrder){

        String tables = new StringBuilder(DbObject.TABLE)
            .append(" LEFT JOIN ")
            .append(Group.TABLE)
            .append(" ON ")
            .append(DbObject.TABLE)
            .append(".")
            .append(DbObject.FEED_NAME)
            .append(" = ")
            .append(Group.TABLE)
            .append(".")
            .append(Group.FEED_NAME)
            .toString();

        // Ignore "secondary feeds" such as application-specific feeds.
        StringBuilder removeChildren = new StringBuilder();
        removeChildren.append(DbObject.TABLE).append(".").append(DbObject.FEED_NAME)
            .append(" NOT IN ('direct','friend') AND ");
        removeChildren.append(DbObject.TABLE).append(".").append(DbObject.FEED_NAME)
            .append(" NOT IN (SELECT ").append(DbObject.CHILD_FEED_NAME)
            .append(" FROM ").append(DbObject.TABLE)
            .append(" WHERE ").append(DbObject.CHILD_FEED_NAME).append(" IS NOT NULL)");
        selection = andClauses(selection, removeChildren.toString());

        String groupBy = DbObject.TABLE + "." + DbObject.FEED_NAME;
        if (sortOrder == null) {
            sortOrder = DbObject.TIMESTAMP + " desc";
        }
        return getReadableDatabase().query(tables, projection, selection, selectionArgs,
                groupBy, null, sortOrder, null);
    }

    public Cursor queryFeed(String realAppId, String feedName, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        String select = andClauses(selection, DbObject.FEED_NAME + "='" + feedName + "'");
        if (!realAppId.equals(DungBeetleContentProvider.SUPER_APP_ID)) {
            select = andClauses(select, DbObject.APP_ID + "='" + realAppId + "'");
        }

        return getReadableDatabase().query(DbObject.TABLE, projection, select, selectionArgs,
                null, null, sortOrder, null);
    }

    Cursor getFeedDependencies(String feedName) {
        String table = DbObject.TABLE;
        String[] columns = new String[] { DbObject.FEED_NAME };
        String selection = DbObject.CHILD_FEED_NAME + " = ?";
        String[] selectionArgs = new String[] { feedName };
        String groupBy = DbObject.FEED_NAME;
        return getReadableDatabase().query(
                table, columns, selection, selectionArgs, groupBy, null, null);
    }

    public Cursor queryFeedLatest(String appId,
                                  String feedName, 
                                  String[] proj, String selection,
                                  String[] selectionArgs, String sortOrder){
        String select = andClauses(selection, DbObject.FEED_NAME + "='" + feedName + "'");
        select = andClauses(select, DbObject.APP_ID + "='" + appId + "'");

        // Don't allow custom projection. Just grab everything.
        String[] projection = new String[]{
            "o." + DbObject._ID + " as " + DbObject._ID,
            "o." + DbObject.TYPE + " as " + DbObject.TYPE,
            "o." + DbObject.SEQUENCE_ID + " as " + DbObject.SEQUENCE_ID,
            "o." + DbObject.FEED_NAME + " as " + DbObject.FEED_NAME,
            "o." + DbObject.CONTACT_ID + " as " + DbObject.CONTACT_ID,
            "o." + DbObject.DESTINATION + " as " + DbObject.DESTINATION,
            "o." + DbObject.JSON + " as " + DbObject.JSON,
            "o." + DbObject.TIMESTAMP + " as " + DbObject.TIMESTAMP,
            "o." + DbObject.APP_ID + " as " + DbObject.APP_ID
        };

        // Double this because select appears twice in full query
        String[] selectArgs = selectionArgs == null ? 
            new String[]{} : concat(selectionArgs, selectionArgs);
        String orderBy = sortOrder == null ? "" : " ORDER BY " + sortOrder;
        String q = joinWithSpaces("SELECT",projToStr(projection),
                                  "FROM (SELECT ", DbObject.CONTACT_ID, ",",
                                  "max(",DbObject.SEQUENCE_ID,")", "as max_seq_id", 
                                  "FROM", DbObject.TABLE,"WHERE",select,"GROUP BY",
                                  DbObject.CONTACT_ID,") AS x INNER JOIN ",
                                  "(SELECT * FROM ",DbObject.TABLE,
                                  "WHERE", select,") AS o ON ",
                                  "o.",DbObject.CONTACT_ID,"=", 
                                  "x.",DbObject.CONTACT_ID,"AND",
                                  "o.",DbObject.SEQUENCE_ID,"=x.max_seq_id",
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

    public Cursor queryUnsentObjects() {
        return getReadableDatabase().query(
            DbObject.TABLE,
            new String[]{ DbObject._ID, DbObject.JSON,
                          DbObject.DESTINATION,
                          DbObject.FEED_NAME,
                          DbObject.ENCODED},
            DbObject.CONTACT_ID + "=? AND " + DbObject.SENT + "=?",
            new String[]{ String.valueOf(Contact.MY_ID), String.valueOf(0) },
            null,
            null,
            "timestamp DESC");
    }

    public boolean queryAlreadyReceived(byte[] encoded) {
        Cursor c = getReadableDatabase().query(
            DbObject.TABLE,
            new String[]{ DbObject._ID },
            DbObject.ENCODED + "= X'" + new String(Hex.encodeHex(encoded)) + "'",
            null,
            null,
            null,
            "timestamp DESC");
        c.moveToFirst();
        if(!c.isAfterLast()) {
        	return true;
        } else {
        	return false;
        }
    }
    public Cursor queryDynamicGroups() {
        return getReadableDatabase().query(
            Group.TABLE,
            null,
            Group.DYN_UPDATE_URI + " is not NULL",
            new String[]{ },
            null,
            null,
            null);
    }


    public void markObjectAsSent(long id) {
        ContentValues cv = new ContentValues();
        cv.put(DbObject.SENT, 1);
        getWritableDatabase().update(
            DbObject.TABLE, 
            cv,
            DbObject._ID + " = " + id,
            null);
    }
    public void markObjectsAsSent(Collection<Long> ids) {
        ContentValues cv = new ContentValues();
        cv.put(DbObject.SENT, 1);
        getWritableDatabase().update(
            DbObject.TABLE, 
            cv,
            DbObject._ID + " in (" + Util.joinLongs(ids,",") + ")",
            null);
    }
    
    public Cursor queryGroupsMembership(long contactId) {
        return getReadableDatabase().query(
            GroupMember.TABLE,
            new String[]{ GroupMember._ID, GroupMember.GROUP_ID },
            GroupMember.CONTACT_ID + "=?",
            new String[]{ String.valueOf(contactId) },
            null,
            null,
            null);
    }
    
    public Cursor queryGroupContacts(long groupId) {
    	return getReadableDatabase().rawQuery(
            " SELECT C.* " + 
            " FROM " + Contact.TABLE + " C, " + 
            GroupMember.TABLE + " G WHERE " + 
            "G." + GroupMember.GROUP_ID + "= ? " + 
            "AND " + 
            "C." + Contact._ID + " = G." + GroupMember.CONTACT_ID,
            new String[] { String.valueOf(groupId) });
    }


	public Maybe<Contact> contactForPersonId(String personId){
        List<Contact> cs = contactsForPersonIds(Collections.singletonList(personId));
        if(!cs.isEmpty()) return Maybe.definitely(cs.get(0));
        else return Maybe.unknown();
    }

	public Maybe<Contact> contactForContactId(Long id) {
        List<Contact> cs = contactsForContactIds(Collections.singletonList(id));
        if(!cs.isEmpty()) return Maybe.definitely(cs.get(0));
        else return Maybe.unknown();
    }

	public List<Contact> contactsForContactIds(Collection<Long> contactIds){
        String idList = Util.joinLongs(contactIds, ",");
        Cursor c = getReadableDatabase().query(
            Contact.TABLE,
            null,
            Contact._ID + " in (" + idList + ")",
            null,null,null,null);
        c.moveToFirst();
        ArrayList<Contact> result = new ArrayList<Contact>();
        while(!c.isAfterLast()){
            result.add(new Contact(c));
            c.moveToNext();
        }
        c.close();
        return result;
    }

	public List<Contact> contactsForPersonIds(Collection<String> personIds){
        Iterator<String> iter = personIds.iterator();
        StringBuffer buffer = new StringBuffer();
        while (iter.hasNext()) {
            buffer.append("'" + iter.next() + "'");
            if(iter.hasNext()){
                buffer.append(",");
            }
        }
        String idList = buffer.toString();
        Cursor c = getReadableDatabase().query(
            Contact.TABLE,
            null,
            Contact.PERSON_ID + " in (" + idList + ")",
            null,null,null,null);
        c.moveToFirst();
        ArrayList<Contact> result = new ArrayList<Contact>();
        while(!c.isAfterLast()){
            result.add(new Contact(c));
            c.moveToNext();
        }
        c.close();
        return result;
    }

	public Maybe<Group> groupForGroupId(long groupId){
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group._ID + "=?",
            new String[]{String.valueOf(groupId)},
            null,null,null);
        c.moveToFirst();
        if(c.isAfterLast()) return Maybe.unknown();
        else return Maybe.definitely(new Group(c));
    }

	public Maybe<Group> groupForFeedName(String feed){
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group.FEED_NAME + "=?",
            new String[]{String.valueOf(feed)},
            null,null,null);
        c.moveToFirst();
        if(c.isAfterLast()) return Maybe.unknown();
        else return Maybe.definitely(new Group(c));
    }


	public Maybe<Group> groupByFeedName(String feedName){
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group.FEED_NAME + "=?",
            new String[]{feedName},
            null,null,null);
        c.moveToFirst();
        if(c.isAfterLast()) return Maybe.unknown();
        else return Maybe.definitely(new Group(c));
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

	public void markEncoded(long id, byte[] encoded) {
        ContentValues cv = new ContentValues();
        cv.put(DbObject.ENCODED, encoded);
        getWritableDatabase().update(
            DbObject.TABLE, 
            cv,
            DbObject._ID + " = " + id,
            null);
	}
    public Map<byte[], byte[]> getPublicKeySharedSecretMap() {
    	HashMap<byte[], byte[]> key_ss = new HashMap<byte[], byte[]>();
        Cursor c = getReadableDatabase().query(
                Contact.TABLE, 
                new String[] {Contact._ID, Contact.PUBLIC_KEY, Contact.SHARED_SECRET},
                null, null,null,null,null);
        c.moveToFirst();
        while(!c.isAfterLast()){
        	byte[] pk = c.getBlob(1);
        	byte[] ss = c.getBlob(2);
        	if(ss == null) {
        		Contact contact;
				try {
					contact = contactForContactId(c.getLong(0)).get();
	        		ss = SharedSecretObj.getOrPushSecret(mContext, contact);
				} catch (NoValError e) {
					e.printStackTrace();
				}
        	}
        	key_ss.put(pk, ss);
            c.moveToNext();
        }
        c.close();
        return key_ss;	
    }
    public void updateNearby(Set<byte[]> nearby) {
    	StringBuilder kl = new StringBuilder(" ");
    	for (byte[] bs : nearby) {
			kl.append("'");
			kl.append(Hex.encodeHex(bs));
			//WTF- this hex encoder suxs and adds extra 00s at the end
			kl.delete(kl.length() - 2, kl.length());
			kl.append("'");
			kl.append(",");
		}
    	getWritableDatabase().execSQL("UPDATE " + Contact.TABLE + " SET nearby = HEX(" + Contact.PUBLIC_KEY + ") in (" + kl.substring(0, kl.length() - 1).toUpperCase() +  ")");
    }
 }
