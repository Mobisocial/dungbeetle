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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobisocial.socialkit.musubi.DbObj;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.SharedSecretObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbRelation;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.obj.handler.FeedModifiedObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.ObjHandler;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = "DBHelper";
	private static final boolean DBG = true;
	public static final String DB_NAME = "MUSUBI.db";
	//for legacy purposes
	public static final String OLD_DB_NAME = "DUNG_HEAP.db";
	public static final String DB_PATH = "/data/edu.stanford.mobisocial.dungbeetle/databases/";
	public static final int VERSION = 54;
	public static final int SIZE_LIMIT = 480 * 1024;
    private final Context mContext;
    private long mNextId = -1;
    private ObjHandler mModifiedHandler;

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
	public static DBHelper getGlobal(Context context) {
		ContentProviderClient cpc = context.getContentResolver().acquireContentProviderClient(DungBeetleContentProvider.CONTENT_URI);
		try {
			DungBeetleContentProvider dbcp = (DungBeetleContentProvider)cpc.getLocalContentProvider();
			return dbcp.getDBHelper();
		} finally {
			cpc.release();
		}
	}
    public synchronized long getNextId() {
    	if(mNextId == -1) {
    		Cursor c = getReadableDatabase().query(DbObject.TABLE, new String[] {"MAX(" + DbObject._ID + ")"}, null, null, null, null, null);
    		try {
    			if(c.moveToFirst()) {
    				mNextId = c.getLong(0) + 1;
    			}
    		} finally {
    			c.close();
    		}
    	}
		return mNextId++;
    }
	private int mRefs = 1;
	public synchronized void addRef() {
		++mRefs;
	}
	@Override
	public synchronized void close() {
		if(--mRefs == 0) {
			super.close();
		}
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
            Intent DBServiceIntent = new Intent(mContext, DungBeetleService.class);
            mContext.stopService(DBServiceIntent);
            mContext.startService(DBServiceIntent);
            return true;
        }
        return false;
    }

	@Override
	public void onOpen(SQLiteDatabase db) {
        // enable locking so we can safely share 
        // this instance around
        db.setLockingEnabled(true);
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
        if(oldVersion <= 35) {
            Log.w(TAG, "Adding column 'last_updated' to group table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.LAST_UPDATED + " INTEGER");
        }
        if (oldVersion <= 36) {
            // Can't easily drop columns, but 'update_id' and 'is_child_feed' are dead columns.

            Log.w(TAG, "Adding column 'parent_feed_id' to group table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.PARENT_FEED_ID + " INTEGER DEFAULT -1");

            Log.w(TAG, "Adding column 'last_object_id' to group table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.LAST_OBJECT_ID + " INTEGER DEFAULT -1");
        }
        if (oldVersion <= 37) {
            // Can't easily drop columns, but 'update_id' and 'is_child_feed' are dead columns.

            Log.w(TAG, "Adding column 'num_unread' to group table.");
            db.execSQL("ALTER TABLE " + Group.TABLE + " ADD COLUMN " + Group.NUM_UNREAD + " INTEGER DEFAULT 0");
        }
        if(oldVersion <= 38) {
            Log.w(TAG, "Adding column 'raw' to object table.");
            db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.RAW + " BLOB");
        }
        // sadly, we have to do this again because incoming voice obj's were not being split!
        if(oldVersion <= 50) {
            Log.w(TAG, "Converting voice and picture objs to raw.");

          Log.w(TAG, "Converting objs to raw.");
          Cursor c = db.query(DbObject.TABLE, new String[] {DbObject._ID}, DbObject.TYPE + " = ? AND " + DbObject.RAW + " IS NULL", new String[] { PictureObj.TYPE }, null, null, null);
          ArrayList<Long> ids = new ArrayList<Long>();            
          if(c.moveToFirst()) do {
      			ids.add(c.getLong(0));
          } while(c.moveToNext());
          c.close();
          DbEntryHandler dbh = DbObjects.forType(PictureObj.TYPE);
          for(Long id : ids) {
	            c = db.query(DbObject.TABLE, new String[] {DbObject.JSON, DbObject.RAW}, DbObject._ID + " = ? ", new String[] { String.valueOf(id.longValue()) }, null, null, null);
	            if(c.moveToFirst()) try {
	            	String json = c.getString(0);
	            	byte[] raw = c.getBlob(1);
	            	c.close();
	            	if(raw == null) {
	            		Pair<JSONObject, byte[]> p = dbh.splitRaw(new JSONObject(json));
	            		if(p != null) {
	            			json = p.first.toString();
	            			raw = p.second;
	            			updateJsonAndRaw(db, id, json, raw);
	            		}
	            	}
      		} catch(JSONException e) {}
	            c.close();
          }
          c = db.query(DbObject.TABLE, new String[] {DbObject._ID}, DbObject.TYPE + " = ? AND " + DbObject.RAW + " IS NULL", new String[] { VoiceObj.TYPE }, null, null, null);
          ids = new ArrayList<Long>();            
          if(c.moveToFirst()) do {
      			ids.add(c.getLong(0));
          } while(c.moveToNext());
          c.close();
          dbh = DbObjects.forType(VoiceObj.TYPE);
          for(Long id : ids) {
	            c = db.query(DbObject.TABLE, new String[] {DbObject.JSON, DbObject.RAW}, DbObject._ID + " = ? ", new String[] { String.valueOf(id.longValue()) }, null, null, null);
	            if(c.moveToFirst()) try {
	            	String json = c.getString(0);
	            	byte[] raw = c.getBlob(1);
	            	c.close();
	            	if(raw == null) {
	            		Pair<JSONObject, byte[]> p = dbh.splitRaw(new JSONObject(json));
	            		if(p != null) {
	            			json = p.first.toString();
	            			raw = p.second;
	            			updateJsonAndRaw(db, id, json, raw);
	            		}
	            	}
      		} catch(JSONException e) {}
	            c.close();
          	}
            
            
        }
        if(oldVersion <= 40) {
            Log.w(TAG, "Adding column 'E' to object table.");
            db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.HASH+ " INTEGER");
            createIndex(db, "INDEX", "objects_by_hash", DbObject.TABLE, DbObject.HASH);
            db.execSQL("DROP INDEX objects_by_encoded");
            db.delete(DbObject.TABLE, DbObject.TYPE + " = ?", new String[] {"profile"});
            db.delete(DbObject.TABLE, DbObject.TYPE + " = ?", new String[] {"profilepicture"});
            ContentValues cv = new ContentValues();
            cv.putNull(DbObject.ENCODED);
            db.update(DbObject.TABLE, cv, null, null);
        }
        if(oldVersion <= 41) {
            db.execSQL("DROP INDEX objects_by_sequence_id");
            db.execSQL("CREATE INDEX objects_by_sequence_id ON " + DbObject.TABLE + "(" + DbObject.CONTACT_ID + ", " + DbObject.FEED_NAME + ", " + DbObject.SEQUENCE_ID + ")");
        }
        //secret to life, etc
        if(oldVersion <= 42) {
            db.execSQL("DROP INDEX objects_by_creator_id");
            db.execSQL("CREATE INDEX objects_by_creator_id ON " + DbObject.TABLE + "(" + DbObject.CONTACT_ID + ", " + DbObject.SENT + ")");
        }

        if (oldVersion <= 44) {
            // oops.
            db.execSQL("DROP TABLE IF EXISTS " + DbRelation.TABLE);
            createRelationBaseTable(db);
        }
        if (oldVersion <= 45) {
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.LAST_OBJECT_ID + " INTEGER");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.LAST_UPDATED + " INTEGER");
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.NUM_UNREAD + " INTEGER DEFAULT 0");
        }
        if (oldVersion <= 46) {
        	db.execSQL("ALTER TABLE " + DbObject.TABLE + " ADD COLUMN " + DbObject.DELETED + " INTEGER DEFAULT 0");
        }
        if (oldVersion <= 47) {
            addRelationIndexes(db);
        }
        if (oldVersion <= 44) {
            createUserAttributesTable(db);
        }

        if (oldVersion <= 49) {
            if (oldVersion > 44) {
                db.execSQL("ALTER TABLE " + DbRelation.TABLE + " ADD COLUMN " + DbRelation.RELATION_TYPE + " TEXT");
                createIndex(db, "INDEX", "relations_by_type", DbRelation.TABLE, DbRelation.RELATION_TYPE);
            }
            db.execSQL("UPDATE " + DbRelation.TABLE + " SET " + DbRelation.RELATION_TYPE + " = 'parent'");
        }
        if(oldVersion <= 52) {
            Log.w(TAG, "Adding column 'about' to my_info table.");
            try {
            	db.execSQL("ALTER TABLE " + MyInfo.TABLE + " ADD COLUMN " + MyInfo.ABOUT + " TEXT DEFAULT ''");
            } catch(Exception e) {
            	// because of bad update, we just ignore the duplicate column error
            }
        }
        if (oldVersion <= 53) {
            db.execSQL("ALTER TABLE " + Contact.TABLE + " ADD COLUMN " + Contact.HIDDEN + " INTEGER DEFAULT 0");
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
        db.execSQL("DROP TABLE IF EXISTS " + DbRelation.TABLE);
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
                        MyInfo.PICTURE, "BLOB",
                        MyInfo.ABOUT, "TEXT DEFAULT ''"
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
                        DbObject.DELETED, "INTEGER DEFAULT 0",
                        DbObject.HASH, "INTEGER",
                        DbObject.ENCODED, "BLOB",
                        DbObject.CHILD_FEED_NAME, "TEXT",
                        DbObject.RAW, "BLOB"
                        );
            db.execSQL("CREATE INDEX objects_by_sequence_id ON " + DbObject.TABLE + "(" + DbObject.CONTACT_ID + ", " + DbObject.FEED_NAME + ", " + DbObject.SEQUENCE_ID + ")");
            createIndex(db, "INDEX", "objects_by_feed_name", DbObject.TABLE, DbObject.FEED_NAME);
            db.execSQL("CREATE INDEX objects_by_creator_id ON " + DbObject.TABLE + "(" + DbObject.CONTACT_ID + ", " + DbObject.SENT + ")");
            createIndex(db, "INDEX", "child_feeds", DbObject.TABLE, DbObject.CHILD_FEED_NAME);
            createIndex(db, "INDEX", "objects_by_hash", DbObject.TABLE, DbObject.HASH);

            createTable(db, Contact.TABLE, null,
                        Contact._ID, "INTEGER PRIMARY KEY",
                        Contact.NAME, "TEXT",
                        Contact.PUBLIC_KEY, "TEXT",
                        Contact.SHARED_SECRET, "BLOB",
                        Contact.PERSON_ID, "TEXT",
                        Contact.EMAIL, "TEXT",
                        Contact.PRESENCE, "INTEGER DEFAULT " + Presence.AVAILABLE,
                        Contact.LAST_PRESENCE_TIME, "INTEGER DEFAULT 0",
                        Contact.LAST_OBJECT_ID, "INTEGER",
                        Contact.LAST_UPDATED, "INTEGER",
                        Contact.NUM_UNREAD, "INTEGER DEFAULT 0",
                        Contact.NEARBY, "INTEGER DEFAULT 0",
                        Contact.STATUS, "TEXT",
                        Contact.PICTURE, "BLOB",
                        Contact.HIDDEN, "INTEGER DEFAULT 0");
            createIndex(db, "UNIQUE INDEX", "contacts_by_person_id", Contact.TABLE, Contact.PERSON_ID);


		    createTable(db, Subscriber.TABLE, new String[]{Subscriber.CONTACT_ID, Subscriber.FEED_NAME},
                        Subscriber._ID, "INTEGER PRIMARY KEY",
                        Subscriber.CONTACT_ID, "INTEGER REFERENCES " + Contact.TABLE + "(" + Contact._ID + ") ON DELETE CASCADE",
                        Subscriber.FEED_NAME, "TEXT");
            createIndex(db, "INDEX", "subscribers_by_contact_id", Subscriber.TABLE, Subscriber.CONTACT_ID);

            createGroupBaseTable(db);
            createGroupMemberBaseTable(db);
            createRelationBaseTable(db);
            addRelationIndexes(db);
            createUserAttributesTable(db);
            generateAndStorePersonalInfo(db);

            db.setVersion(VERSION);
            db.setTransactionSuccessful();
            db.endTransaction();
            this.onOpen(db);
        //}
	}

	private final void createGroupBaseTable(SQLiteDatabase db) {
	    createTable(db, Group.TABLE, null,
                Group._ID, "INTEGER PRIMARY KEY",
                Group.NAME, "TEXT",
            Group.FEED_NAME, "TEXT",
            Group.DYN_UPDATE_URI, "TEXT",
            Group.VERSION, "INTEGER DEFAULT -1",
            Group.LAST_UPDATED, "INTEGER",
            Group.LAST_OBJECT_ID, "INTEGER DEFAULT -1",
            Group.PARENT_FEED_ID, "INTEGER DEFAULT -1",
            Group.NUM_UNREAD, "INTEGER DEFAULT 0"
                );
	    createIndex(db, "INDEX", "last_updated", Group.TABLE, Group.LAST_OBJECT_ID);
	}

	private final void createGroupMemberBaseTable(SQLiteDatabase db) {
	    createTable(db, GroupMember.TABLE, null,
                GroupMember._ID, "INTEGER PRIMARY KEY",
                GroupMember.GROUP_ID, "INTEGER REFERENCES " + Group.TABLE + "(" + Group._ID + ") ON DELETE CASCADE",
                GroupMember.CONTACT_ID, "INTEGER REFERENCES " + Contact.TABLE + "(" + Contact._ID + ") ON DELETE CASCADE",
                GroupMember.GLOBAL_CONTACT_ID, "TEXT");
	    createIndex(db, "UNIQUE INDEX", "group_members_by_group_id", GroupMember.TABLE, 
                GroupMember.GROUP_ID + "," + GroupMember.CONTACT_ID);
	}

	private final void createRelationBaseTable(SQLiteDatabase db) {
	    createTable(db, DbRelation.TABLE, null,
                DbRelation._ID, "INTEGER PRIMARY KEY",
                DbRelation.OBJECT_ID_A, "INTEGER",
                DbRelation.OBJECT_ID_B, "INTEGER",
                DbRelation.RELATION_TYPE, "TEXT"
                );
	    createIndex(db, "INDEX", "relations_by_type", DbRelation.TABLE, DbRelation.RELATION_TYPE);
	}

	private final void createUserAttributesTable(SQLiteDatabase db) {
	    // contact_attributes: _id, contact_id, attr_name, attr_value
	    // TODO: genericize; createDbTable(DbTable table) { ... }
	    String[] colNames = DbContactAttributes.getColumnNames();
	    String[] colTypes = DbContactAttributes.getTypeDefs();
	    String[] colDefs = new String[colNames.length * 2];
	    int j = 0;
	    for (int i = 0; i < colNames.length; i += 1) {
	        colDefs[j++] = colNames[i];
	        colDefs[j++] = colTypes[i];
	    }
	    createTable(db, DbContactAttributes.TABLE, null, colDefs);
        createIndex(db, "UNIQUE INDEX", "attrs_by_contact_id", DbContactAttributes.TABLE, DbContactAttributes.CONTACT_ID);
	}

	private final void addRelationIndexes(SQLiteDatabase db) {
	    createIndex(db, "INDEX", "relation_obj_a", DbRelation.TABLE, DbRelation.OBJECT_ID_A);
	    createIndex(db, "INDEX", "relation_obj_b", DbRelation.TABLE, DbRelation.OBJECT_ID_B);
	}

    private void generateAndStorePersonalInfo(SQLiteDatabase db){
        String email = getUserEmail();
        String name = email; // How to get this?

        KeyPair keypair = DBIdentityProvider.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();
        String pubKeyStr = FastBase64.encodeToString(publicKey.getEncoded());
        String privKeyStr = FastBase64.encodeToString(privateKey.getEncoded());
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

    long addToOutgoing(String appId, String to, String type, JSONObject json){
        return addToOutgoing(getWritableDatabase(), 
                             appId, to, type, json);
    }

    void prepareForSending(JSONObject json, String type, long timestamp, String appId)
            throws JSONException {
        json.put("type", type);
        json.put("feedName", "friend");
        json.put("timestamp", timestamp);
        json.put("appId", appId);
    }

    long addToOutgoing(
        SQLiteDatabase db, String appId, String to, String type, JSONObject json) {
        if (DBG) {
            Log.d(TAG, "Adding to outgoing; to: " + to + ", json: " + json);
        }
        try{
            long timestamp = new Date().getTime();
            prepareForSending(json, type, timestamp, appId);
            ContentValues cv = new ContentValues();
            cv.put(DbObject._ID, getNextId());
            cv.put(DbObject.APP_ID, appId);
            cv.put(DbObject.FEED_NAME, "friend");
            cv.put(DbObject.CONTACT_ID, Contact.MY_ID);
            cv.put(DbObject.DESTINATION, to);
            cv.put(DbObject.TYPE, type);
            cv.put(DbObject.JSON, json.toString());
            cv.put(DbObject.SEQUENCE_ID, 0);
            cv.put(DbObject.TIMESTAMP, timestamp);
            if(cv.getAsString(DbObject.JSON).length() > SIZE_LIMIT)
            	throw new RuntimeException("Messasge size is too large for sending");
            db.insertOrThrow(DbObject.TABLE, null, cv);
            return 0;
        }
        catch(Exception e){
            // TODO, too spammy
            //e.printStackTrace(System.err);
            return -1;
        }
    }
    void updateJsonAndRaw(SQLiteDatabase db, long id, String json, byte[] raw) {
    	ContentValues cv = new ContentValues();
    	cv.put(DbObject.JSON, json);
    	cv.put(DbObject.RAW, raw);
    	db.update(DbObject.TABLE, cv, DbObject._ID + " = ?" , new String[] { String.valueOf(id)});
    }
    
    

    long addToFeed(String appId, String feedName, String type, JSONObject json) {
        try {
            long nextSeqId = getFeedMaxSequenceId(Contact.MY_ID, feedName) + 1;
            long timestamp = new Date().getTime();
            json.put(DbObjects.TYPE, type);
            json.put(DbObjects.FEED_NAME, feedName);
            json.put(DbObjects.SEQUENCE_ID, nextSeqId);
            json.put(DbObjects.TIMESTAMP, timestamp);
            json.put(DbObjects.APP_ID, appId);

            ContentValues cv = new ContentValues();
            cv.put(DbObject._ID, getNextId());
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
            if(cv.getAsString(DbObject.JSON).length() > SIZE_LIMIT)
            	throw new RuntimeException("Messasge size is too large for sending");
            Long objId = getWritableDatabase().insertOrThrow(DbObject.TABLE, null, cv);

            if (json.has(DbObjects.TARGET_HASH)) {
                long hashA = json.optLong(DbObjects.TARGET_HASH);
                long idA = objIdForHash(hashA);
                String relation;
                if (json.has(DbObjects.TARGET_RELATION)) {
                    relation = json.optString(DbObjects.TARGET_RELATION);
                } else {
                    relation = DbRelation.RELATION_PARENT;
                }
                if (idA == -1) {
                    Log.e(TAG, "No objId found for hash " + hashA);
                } else {
                    addObjRelation(idA, objId, relation);
                }
            }

            Uri objUri = DbObject.uriForObj(objId);
            mContext.getContentResolver().registerContentObserver(objUri, false, new ModificationObserver(mContext, objId));
            return objId;
        }
        catch(Exception e) {
            // TODO, too spammy
            //e.printStackTrace(System.err);
            return -1;
        }
    }


    long addObjectByJson(long contactId, JSONObject json, long hash, byte[] raw){
        try{
            long objId = getNextId();
            long seqId = json.optLong(DbObjects.SEQUENCE_ID);
            long timestamp = json.getLong(DbObjects.TIMESTAMP);
            String feedName = json.getString(DbObjects.FEED_NAME);
            String type = json.getString(DbObjects.TYPE);
            String appId = json.getString(DbObjects.APP_ID);
            ContentValues cv = new ContentValues();
            cv.put(DbObject._ID, objId);
            cv.put(DbObject.APP_ID, appId);
            cv.put(DbObject.FEED_NAME, feedName);
            cv.put(DbObject.CONTACT_ID, contactId);
            cv.put(DbObject.TYPE, type);
            cv.put(DbObject.SEQUENCE_ID, seqId);
            cv.put(DbObject.JSON, json.toString());
            cv.put(DbObject.TIMESTAMP, timestamp);
            cv.put(DbObject.HASH, hash);
            cv.put(DbObject.SENT, 1);
            cv.put(DbObject.RAW, raw);

            // TODO: Deprecated!!
            if (json.has(DbObject.CHILD_FEED_NAME)) {
                cv.put(DbObject.CHILD_FEED_NAME, json.optString(DbObject.CHILD_FEED_NAME));
            }
            if(cv.getAsString(DbObject.JSON).length() > SIZE_LIMIT)
            	throw new RuntimeException("Messasge size is too large for sending");
            long newObjId = getWritableDatabase().insertOrThrow(DbObject.TABLE, null, cv);

            String notifyName = feedName;
            if (json.has(DbObjects.TARGET_HASH)) {
                long hashA = json.optLong(DbObjects.TARGET_HASH);
                long idA = objIdForHash(hashA);
                notifyName = feedName + ":" + hashA;
                String relation;
                if (json.has(DbObjects.TARGET_RELATION)) {
                    relation = json.optString(DbObjects.TARGET_RELATION);
                } else {
                    relation = DbRelation.RELATION_PARENT;
                }
                if (idA == -1) {
                    Log.e(TAG, "No objId found for hash " + hashA);
                } else {
                    addObjRelation(idA, newObjId, relation);
                }
            }

            ContentResolver resolver = mContext.getContentResolver();
            DungBeetleContentProvider.notifyDependencies(this, resolver, notifyName);
            updateObjModification(App.instance().getMusubi().objForId(newObjId));
            return objId;
        }
        catch(Exception e){
            if (DBG) Log.e(TAG, "Error adding object by json.", e);
            return -1;
        }
    }

    /**
     * Adds a parent/child relation to the database given a child obj.
     * The obj must have a {@link DbObjects#TARGET_HASH} field.
     */
    public void addObjRelation(long idA, long idB, String relation) {
        ContentValues cv = new ContentValues();
        cv.put(DbRelation.OBJECT_ID_A, idA);
        cv.put(DbRelation.OBJECT_ID_B, idB);
        cv.put(DbRelation.RELATION_TYPE, relation);
        getWritableDatabase().insertOrThrow(DbRelation.TABLE, null, cv);
    }

    public long objIdForHash(long hash) {
        Cursor c = getReadableDatabase().query(
                DbObject.TABLE,
                new String[]{ DbObject._ID },
                DbObject.HASH + "= ?",
                new String[] { String.valueOf(hash) },
                null,
                null,
                null);
        try {
	        if (c.moveToFirst()) {
	            return c.getLong(0);
	        }
	        return -1;
        } finally {
        	c.close();
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
            String tag = edu.stanford.mobisocial.bumblebee.util.Util.makePersonIdForPublicKey(key);
            cv.put(Contact.PERSON_ID, tag);
            String name = cv.getAsString(Contact.NAME);
            assert (name != null) && name.length() > 0;
            return getWritableDatabase().insertOrThrow(Contact.TABLE, null, cv);
        }
        catch(Exception e){
            Log.e(TAG, e.getMessage(), e);
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
        } catch (SQLiteConstraintException e) {
        	//this inserts dupes, so hide this spam in a way 
        	//that doesn't require api level 8
        	return -1;
        } catch(Exception e){
            Log.e(TAG, e.getMessage(), e);
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
            Log.e(TAG, e.getMessage(), e);
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
            new String[]{ DbObject.SEQUENCE_ID },
            DbObject.CONTACT_ID + "=? AND " + DbObject.FEED_NAME + "=?",
            new String[]{ String.valueOf(contactId), feedName },
            null,
            null,
            DbObject.SEQUENCE_ID + " DESC LIMIT 1");
        try {
            c.moveToFirst();
	        if(!c.isAfterLast()){
	            long max = c.getLong(0);
	            Log.i(TAG, "Found max seq num: " + max);
	            return max;
	        }
	        return -1;
        } finally {
            c.close();
        }
    }

    private long getFeedLastVisibleId(String feedName) {
        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        String visibleTypes =  DbObject.TYPE + " in (" + allowed.substring(1) + ")";
        String selection = DbObject.FEED_NAME + " = ?";

        selection = andClauses(selection, visibleTypes);
        Cursor c = getReadableDatabase().query(DbObject.TABLE, new String[] { DbObject._ID },
            selection, new String[]{ feedName }, null, null,
            DbObject.SEQUENCE_ID + " DESC LIMIT 1");
        try {
            c.moveToFirst();
            if(!c.isAfterLast()){
                long max = c.getLong(0);
                return max;
            }
            return -1;
        } finally {
            c.close();
        }
    }

    public Cursor queryFeedList(String[] projection, String selection, String[] selectionArgs,
            String sortOrder){

        /*return getReadableDatabase().rawQuery("SELECT * 
            FROM Group.TABLE, DBObject.TABLE
            WHERE Group.IS_CHILD_FEED != 1 AND Group.TABLE + "." + Group.LAST_OBJECT_ID = DBObject.TABLE + "." DBObject._ID
            ORDER BY Group.LAST_UPDATED DESC");*/

        ContentResolver resolver = mContext.getContentResolver();

        String tables = Group.TABLE + ", " + DbObject.TABLE;
        String selection2 = Group.TABLE + "." + Group.PARENT_FEED_ID + " = -1 " +
                    " AND " + Group.TABLE + "." + Group.LAST_OBJECT_ID + " = " + DbObject.TABLE + "." + DbObject._ID;
        selection = andClauses(selection, selection2);
        selectionArgs = null;
        if (sortOrder == null) {
            sortOrder = Group.LAST_UPDATED + " DESC";
        }

        Cursor c = getReadableDatabase().query(tables, projection, selection, selectionArgs,
                null, null, sortOrder, null);
        c.setNotificationUri(resolver, Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist"));
        return c;
    }

    public Cursor queryFeed(String realAppId, String feedName, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "Querying feed: " + feedName);
        String objHashStr = null;
        if (feedName.contains(":")) {
            String[] contentParts = feedName.split(":");
            if (contentParts.length != 2) {
                Log.e(TAG, "Error parsing feed::: " + feedName);
            } else {
                feedName = contentParts[0];
                objHashStr = contentParts[1];
            }
        }

        final String ID = DbObject._ID;
        final String OBJECTS = DbObject.TABLE;
        final String RELATIONS = DbRelation.TABLE;
        final String HASH = DbObject.HASH;
        final String OBJECT_ID_A = DbRelation.OBJECT_ID_A;
        final String OBJECT_ID_B = DbRelation.OBJECT_ID_B;
        String select = andClauses(selection, DbObject.FEED_NAME + " = '" + feedName + "'");
        if (objHashStr != null) {
            // sql injection security:
            Long objHash = Long.parseLong(objHashStr);
            String objIdSearch =
                    "(SELECT " + ID +
                    " FROM " + OBJECTS +
                    " WHERE " + HASH + " = " + objHash + ")";
            select = andClauses(select, "(" + ID + " IN (SELECT " +
                    OBJECT_ID_B + " FROM " + RELATIONS + " WHERE " +
                    OBJECT_ID_A + " = " + objIdSearch + " ) OR " + HASH + " = " + objHash + ")");
        } else {
            select = andClauses(select, ID + " NOT IN (SELECT " +
                        DbRelation.OBJECT_ID_B + " FROM " + DbRelation.TABLE +
                        " WHERE " + DbRelation.RELATION_TYPE + " IN ('parent'))");
        }
        if (!realAppId.equals(DungBeetleContentProvider.SUPER_APP_ID)) {
            select = andClauses(select, DbObject.APP_ID + "='" + realAppId + "'");
        }
        if (DBG) {
            Log.d(TAG, "Running query " + select);
            String args = "";
            if (selectionArgs != null) {
                for (String arg : selectionArgs) {
                    args += ", " + arg;
                }
                Log.d(TAG, "args: " + args.substring(2));
            }
        }
        Cursor c = getReadableDatabase().query(DbObject.TABLE, projection, select, selectionArgs,
                null, null, sortOrder, null);
        if (DBG) Log.d(TAG, "got " + c.getCount() + " items");
        return c;
    }

    public Cursor queryFriend(String realAppId, Long contactId, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        StringBuilder friendFilter = new StringBuilder();
        friendFilter.append(DbObject.FEED_NAME + " = 'friend'");
        friendFilter.append(" AND ((").append(DbObject.DESTINATION)
            .append(" = " + contactId +" AND ").append(DbObject.CONTACT_ID)
            .append(" = " + Contact.MY_ID + " ) OR (")
            .append(DbObject.DESTINATION).append(" is null AND ")
            .append(DbObject.CONTACT_ID).append(" = " + contactId + "))"); 
        String select = andClauses(selection, friendFilter.toString());
        if (!realAppId.equals(DungBeetleContentProvider.SUPER_APP_ID)) {
            select = andClauses(select, DbObject.APP_ID + "='" + realAppId + "'");
        }
        if (DBG) Log.d(TAG, "Friend query selection: " + select);
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

        // TODO: allow federated permission across apps.
        if (!DungBeetleContentProvider.SUPER_APP_ID.equals(appId)) {
            select = andClauses(select, DbObject.APP_ID + "='" + appId + "'");
        }

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
            new String[]{} : andArguments(selectionArgs, selectionArgs);
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

    public Cursor queryUnsentObjects(long max_sent) {
    	//TODO: fix indexes again
        return getReadableDatabase().query(
            DbObject.TABLE,
            new String[]{ DbObject._ID,
            			  DbObject.ENCODED + " IS NOT NULL AS is_encoded",
            			  DbObject.TYPE,
            			  DbObject.SENT,
            			  DbObject.JSON,
                          DbObject.DESTINATION,
                          DbObject.FEED_NAME,
                        },
            DbObject.CONTACT_ID + "=? AND " + DbObject.SENT + "=? AND " + DbObject._ID + ">?",
            new String[]{ String.valueOf(Contact.MY_ID), String.valueOf(0), String.valueOf(max_sent)},
            null,
            null,
            DbObject._ID + " ASC");
    }

    public boolean queryAlreadyReceived(long hash) {
        Cursor c = getReadableDatabase().query(
            DbObject.TABLE,
            new String[]{ DbObject._ID },
            DbObject.HASH + "= ?",
            new String[] { String.valueOf(hash) },
            null,
            null,
            null);
        try {
	        if(c.moveToFirst()) {
	        	return true;
	        } else {
	        	return false;
	        }
        } finally {
        	c.close();
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


    public void markFeedAsRead(String feedName) {
        ContentValues cv = new ContentValues();
        cv.put(Group.NUM_UNREAD, 0);
        getWritableDatabase().update(
        		Group.TABLE, 
            cv,
            Group.FEED_NAME+"='"+feedName+"'",
            null);
    }
    public void markContactAsRead(long contact_id) {
        ContentValues cv = new ContentValues();
        cv.put(Contact.NUM_UNREAD, 0);
        getWritableDatabase().update(
        		Contact.TABLE, 
            cv,
            Contact._ID+"='"+contact_id+"'",
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
            "C." + Contact._ID + " = G." + GroupMember.CONTACT_ID + 
            " ORDER BY " + Contact.NAME + " COLLATE NOCASE ASC",
            new String[] { String.valueOf(groupId) });
    }

    public Cursor queryFeedMembers(String[] projection, String selection, String[] selectionArgs,
            String feedName, String appId) {
        // TODO: Check appId against feed?

        String[] realSelectionArgs = null;
        String feedInnerQuery = new StringBuilder()
            .append("SELECT M." + GroupMember.CONTACT_ID)
            .append(" FROM " + GroupMember.TABLE + " M, ")
            .append(Group.TABLE + " G")
            .append(" WHERE ")
            .append("M." + GroupMember.GROUP_ID + " = G." + Group._ID)
            .append(" AND ")
            .append("G." + Group.FEED_NAME + " = ?").toString();
        String forFeed = Contact._ID + " IN ( " + feedInnerQuery + ")";

        if (selection == null) {
            selection = forFeed;
            realSelectionArgs = new String[] { feedName };
        } else {
            selection = andClauses(selection, forFeed);
            if (selectionArgs == null) {
                realSelectionArgs = new String[] { feedName };
            } else {
                realSelectionArgs = new String[selectionArgs.length + 1];
                System.arraycopy(selectionArgs, 0, realSelectionArgs, 0, selectionArgs.length);
                realSelectionArgs[selectionArgs.length] = feedName;
            }
        }

        String groupBy = null;
        String having = null;
        String orderBy = null;
        return getReadableDatabase().query(
                Contact.TABLE,
                projection,
                selection,
                realSelectionArgs,
                groupBy,
                having,
                orderBy);
    }

    public Cursor queryFeedMembers(String feedName) {
        // TODO: Check appId against database.
        String query = new StringBuilder()
            .append("SELECT C.*")
            .append(" FROM " + Contact.TABLE + " C, ")
            .append(GroupMember.TABLE + " M, ")
            .append(Group.TABLE + " G")
            .append(" WHERE ")
            .append("M." + GroupMember.GROUP_ID + " = G." + Group._ID)
            .append(" AND ")
            .append("G." + Group.FEED_NAME + " = ? AND " )
            .append("C." + Contact._ID + " = M." + GroupMember.CONTACT_ID)
            .toString();
        return getReadableDatabase().rawQuery(query,
                new String[] { feedName });
    }

    public Cursor queryMemberDetails(String feedName, String personId) {
        // TODO: Check appId against database.
        String query = new StringBuilder()
            .append("SELECT C.*")
            .append(" FROM " + Contact.TABLE + " C ")
            .append(" WHERE ")
            .append("C." + Contact.PERSON_ID + " = ?")
            .toString();
        return getReadableDatabase().rawQuery(query,
                new String[] { personId });
    }

    public Cursor queryGroups() {
        String selection = DbObject.FEED_NAME + " not in " +
                "(select " + DbObject.CHILD_FEED_NAME + " from " + DbObject.TABLE +
                " where " + DbObject.CHILD_FEED_NAME + " is not null)";
        String[] selectionArgs = null;
        //Cursor c = getReadableDatabase().query(Group.TABLE, null, selection, null, null, Group.NAME + " ASC", null);
        Cursor c = getReadableDatabase().query(Group.TABLE, null, selection, selectionArgs, null, null, Group.NAME + " COLLATE NOCASE ASC", null);
        return c;
    }

    public Cursor queryLocalUser(String feed_name) {
        String table = MyInfo.TABLE;
        String[] columns = new String[] { MyInfo._ID, MyInfo.NAME, MyInfo.PICTURE, MyInfo.PUBLIC_KEY };
        String selection = null;
        String selectionArgs[] = null;
        String groupBy = null;
        String having = null;
        String orderBy = null;
        return getReadableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
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
        try {
	        ;
	        ArrayList<Contact> result = new ArrayList<Contact>();
	        if(c.moveToFirst()) do {
	            result.add(new Contact(c));
	        } while(c.moveToNext());
	        return result;
        } finally {
        	c.close();
        }
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
        try {
	        ArrayList<Contact> result = new ArrayList<Contact>();
	        if(c.moveToFirst()) do {
	            result.add(new Contact(c));
	        } while(c.moveToNext());
	        return result;
        } finally {
        	c.close();
        }
    }

	public Maybe<Group> groupForGroupId(long groupId){
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group._ID + "=?",
            new String[]{String.valueOf(groupId)},
            null,null,null);
        try {
	        Maybe<Group> mg;
	        if (!c.moveToFirst()) {
	            mg = Maybe.unknown();
	        } else { 
	            mg = Maybe.definitely(new Group(c));
	        }
	        return mg;
        } finally {
        	c.close();
        }
    }

	public Maybe<Group> groupForFeedName(String feed){
	    
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group.FEED_NAME + "=?",
            new String[]{String.valueOf(feed)},
            null,null,null);
        try {
	        Maybe<Group> mg;
	        if (!c.moveToFirst()) {
	            mg = Maybe.unknown();
	        } else { 
	            mg = Maybe.definitely(new Group(c));
	        }
	        return mg;
        } finally {
        	c.close();
        }
    }

	public Maybe<Group> groupByFeedName(String feedName){
        Cursor c = getReadableDatabase().query(
            Group.TABLE,
            null,
            Group.FEED_NAME + "=?",
            new String[]{feedName},
            null,null,null);
        try {
	        if(!c.moveToFirst()) return Maybe.unknown();
	        else return Maybe.definitely(new Group(c));
        } finally {
        	c.close();
        }
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

    public static String[] andArguments(String[] A, String[] B) {
        if (A == null) return B;
        if (B == null) return A;
        String[] C = new String[A.length + B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);
        return C;
    }

	public void markEncoded(long id, byte[] encoded, String json, byte[] raw, long hash) {
        ContentValues cv = new ContentValues();
        cv.put(DbObject.ENCODED, encoded);
        cv.put(DbObject.JSON, json);
        cv.put(DbObject.RAW, raw);
        cv.put(DbObject.HASH, hash);
        getWritableDatabase().update(
            DbObject.TABLE, 
            cv,
            DbObject._ID + " = " + id,
            null);
        Uri objUri = DbObject.uriForObj(id);
        mContext.getContentResolver().notifyChange(objUri, null);
	}

	public byte[] getEncoded(long id) {
        Cursor c = getReadableDatabase().query(
                DbObject.TABLE,
                new String[]{ DbObject.ENCODED },
                DbObject._ID + "=?",
                new String[]{ String.valueOf(id) },
                null,
                null,
                null);

        try {
            if(!c.moveToFirst())
            	return null;
        	return c.getBlob(0);
        } finally {
        	c.close();
        }
	}
	//gets all known people's id's, in other words, their public keys.
    public Set<byte[]> getPublicKeys() {
    	HashSet<byte[]> key_ss = new HashSet<byte[]>();
        Cursor c = getReadableDatabase().query(
                Contact.TABLE, 
                new String[] {Contact._ID, Contact.PUBLIC_KEY},
                null, null,null,null,null);
        try {
	        if(c.moveToFirst()) do {
	        	byte[] pk = c.getBlob(1);
	        	key_ss.add(pk);
	        } while(c.moveToNext());
	        return key_ss;	
        } finally {
        	c.close();
        }
    }
    //gets the shared secret for all contacts.
    public Map<byte[], byte[]> getPublicKeySharedSecretMap() {
    	HashMap<byte[], byte[]> key_ss = new HashMap<byte[], byte[]>();
        Cursor c = getReadableDatabase().query(
                Contact.TABLE, 
                new String[] {Contact._ID, Contact.PUBLIC_KEY, Contact.SHARED_SECRET},
                null, null,null,null,null);
        try {
	        if(c.moveToFirst()) do {
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
	        } while(c.moveToNext());
	        return key_ss;	
        } finally {
        	c.close();
        }
    }
	//gets the shared secret with one specific contact or create a shared secret if there is none... null if the public key is unknown
    public byte[] getSharedSecret(byte[] public_key) {
    	String hex = new String(Hex.encodeHex(public_key));
    	hex = hex.substring(0, hex.length() - 2);
    	hex = hex.toUpperCase();
        Cursor c = getReadableDatabase().rawQuery("SELECT " + Contact._ID + "," + Contact.SHARED_SECRET + " FROM " +
        		Contact.TABLE + " WHERE HEX(" + Contact.PUBLIC_KEY + ") = '" + hex + "'", 
        		null);
        try {
	        if(!c.moveToFirst()) {
	        	// no such person
	        	return null;
	        }
	        byte[] ss = c.getBlob(1);
	    	long id = c.getLong(0);

	    	if(ss != null) {
	        	return ss;	
	        }
			Contact contact;
			try {
				contact = contactForContactId(id).get();
	    		return SharedSecretObj.getOrPushSecret(mContext, contact);
			} catch (NoValError e) {
				return null;
			}
        } finally {
        	c.close();
        }
    }
	//gets the contact for a public key
    public Contact getContactForPublicKey(byte[] public_key) {
    	String hex = new String(Hex.encodeHex(public_key));
    	hex = hex.substring(0, hex.length() - 2);
    	hex = hex.toUpperCase();
        Cursor c = getReadableDatabase().rawQuery("SELECT " + Contact._ID + " FROM " +
        		Contact.TABLE + " WHERE HEX(" + Contact.PUBLIC_KEY + ") = '" + hex + "'", 
        		null);
        try {
	        if(!c.moveToFirst()) {
	        	// no such person
	        	return null;
	        }
	    	long id = c.getLong(0);
			try {
				return contactForContactId(id).get();
			} catch (NoValError e) {
				return null;
			}
        } finally {
	        c.close();
        }
    }    
    //marks all friends as nearby whose keys are in the specified set.  everyone outside the set is marked not nearby
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
    //control whether one person is nearby or not
    public void setNearby(byte[] public_key, boolean nearby) {
    	String hex = new String(Hex.encodeHex(public_key));
    	hex = hex.substring(0, hex.length() - 2);
    	hex = hex.toUpperCase();
    	getWritableDatabase().execSQL("UPDATE " + Contact.TABLE + " SET nearby = " + (nearby ? "1" : "0") + " WHERE HEX(" + Contact.PUBLIC_KEY + ") = '" + hex + "'");
    }

	public String getDatabasePath() {
		return DB_PATH+DB_NAME;
	}
	public void vacuum() {
		getWritableDatabase().execSQL("VACUUM");
	}

	public void deleteObj(long id) {
		getWritableDatabase().delete(DbObject.TABLE, DbObject._ID + " = ?", new String[] {String.valueOf(id)});
	}

	public void clearEncoded(long id) {
		ContentValues cv = new ContentValues();
		cv.putNull(DbObject.ENCODED);
		getWritableDatabase().update(DbObject.TABLE, cv, DbObject._ID + " = ?", new String[] {String.valueOf(id)});
	}

	public Cursor queryRelatedObjs(long objId) {
	    return queryRelatedObjs(objId, null);
	}

    public Cursor queryRelatedObjs(long objId, String type) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT objB.* FROM ")
            .append(DbObject.TABLE + " objA " + ", " + DbObject.TABLE + " objB, ")
            .append(DbRelation.TABLE + " rel ")
            .append(" WHERE objA." + DbObject._ID + " = ? ")
            .append(" AND objA." + DbObject._ID + " = rel." + DbRelation.OBJECT_ID_A)
            .append(" AND objB." + DbObject._ID + " = rel." + DbRelation.OBJECT_ID_B);
        String[] args;
        if (type != null) {
            args = new String[] { String.valueOf(objId), type };
            sql.append(" AND objB." + DbObject.TYPE + " = ?");
        } else {
            args = new String[] { String.valueOf(objId) };
        }
        return getReadableDatabase().rawQuery(sql.toString(), args);
    }
	public void deleteObjByHash(long hash) {
		getWritableDatabase().delete(DbObject.TABLE, DbObject. HASH + " = ?", new String[] {String.valueOf(hash)});
		
	}
	public void deleteObjByHash(long id, long hash) {
		//TODO: limit by contact and add indexes
		getWritableDatabase().delete(DbObject.TABLE, DbObject.HASH + " = ?", new String[] {String.valueOf(hash)});
	}

	public void deleteObjByHash(Uri feedUri, long hash) {
	    getWritableDatabase().delete(DbObject.TABLE,
	            DbObject.HASH + " = ? AND " + DbObject.FEED_NAME + " = ?",
	            new String[] { String.valueOf(hash), feedUri.getLastPathSegment()});
	}

	public void markOrDeleteFeedObjs(Uri feedUri, long[] hashes, boolean force) {
	    StringBuilder hashBuilder = new StringBuilder();
	    for (long hash : hashes) {
	        hashBuilder.append(",").append(hash);
	    }
	    String hashList = "(" + hashBuilder.substring(1) + ")";
	    String feedName = feedUri.getLastPathSegment();

	    final String FEED = DbObject.FEED_NAME;
	    final String CONTACT = DbObject.CONTACT_ID;
	    final String HASH = DbObject.HASH;

        if (force) {
            String[] selectionArgs = new String[] { feedName };
            getWritableDatabase().delete(DbObject.TABLE,
                    HASH + " in " + hashList + " AND " + FEED + " = ?", selectionArgs);
        } else {
            String[] selectionArgs = new String[] { Long.toString(Contact.MY_ID), feedName };
            getWritableDatabase().delete(DbObject.TABLE,
                    CONTACT + " != ? AND " + HASH + " in " + hashList + " AND " + FEED + " = ?",
                    selectionArgs);

            ContentValues cv = new ContentValues();
            cv.put(DbObject.DELETED, 1);
            getWritableDatabase().update(DbObject.TABLE, cv,
                    CONTACT + " = ? AND " + HASH + " in " + hashList + " AND " + FEED + " = ?",
                    selectionArgs);
        }

        /*
         * Update the feed modification in case the latest obj was deleted.
         */
        long objId = getFeedLastVisibleId(feedName);
        ContentValues modifiedCv = new ContentValues();
        modifiedCv.put(Group.LAST_UPDATED, new Date().getTime());
        modifiedCv.put(Group.LAST_OBJECT_ID, objId);
        int rows = getWritableDatabase().update(Group.TABLE, modifiedCv,
                Group.FEED_NAME + " = ?", new String[] { feedName });
        Uri feedlistUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
        Log.d(TAG, "Updating obj on " + feedName + " with " + objId + ", set " + rows);
        mContext.getContentResolver().notifyChange(feedlistUri, null);
	}

	public void markOrDeleteObjs(long[] hashes) {
        StringBuilder hashBuilder = new StringBuilder();
        for (long hash : hashes) {
            hashBuilder.append(",").append(hash);
        }
        String hashList = "(" + hashBuilder.substring(1) + ")";
        String[] selectionArgs = new String[] { Long.toString(Contact.MY_ID) };

        final String CONTACT = DbObject.CONTACT_ID;
        final String HASH = DbObject.HASH;

        getWritableDatabase().delete(DbObject.TABLE,
                CONTACT + " != ? AND " + HASH + " in " + hashList,
                selectionArgs);

        ContentValues cv = new ContentValues();
        cv.put(DbObject.DELETED, 1);
        getWritableDatabase().update(DbObject.TABLE, cv,
                CONTACT + " = ? AND " + HASH + " in " + hashList,
                selectionArgs);
    }

	public long getObjSenderId(long hash) {
		Cursor c = getReadableDatabase().rawQuery("SELECT " + DbObject.CONTACT_ID + " FROM " +
        		DbObject.TABLE + " WHERE " + DbObject.HASH + " = '" + hash + "'", 
        		null);
		try {
	        if(!c.moveToFirst()) {
	        	// no such person
	        	return -1;
	        }
	    	long id = c.getLong(0);
	    	return id;
		} finally {
			c.close();
		}
	}

	private class ModificationObserver extends ContentObserver {
	    final long mObjId;
	    final Context mContext;
        public ModificationObserver(Context context, long objId) {
            super(new Handler(context.getMainLooper()));
            mContext = context;
            mObjId = objId;
        }

        @Override
        public void onChange(boolean selfChange) {
            mContext.getContentResolver().unregisterContentObserver(this);
            DbObj obj = App.instance().getMusubi().objForId(mObjId);
            updateObjModification(obj);
        }
	}

	void updateObjModification(DbObj obj) {
	    // Lazy loading
        if (mModifiedHandler == null) {
            mModifiedHandler = new FeedModifiedObjHandler(DBHelper.this);
        }
        mModifiedHandler.handleObj(mContext, DbObjects.forType(obj.getType()), obj);
	}
 }
