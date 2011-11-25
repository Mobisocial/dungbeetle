package edu.stanford.mobisocial.dungbeetle;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import mobisocial.socialkit.musubi.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.objects.DeleteObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToGroupObj;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbRelation;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

public class DungBeetleContentProvider extends ContentProvider {
	public static final String AUTHORITY = "org.mobisocial.db";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	static final String TAG = "DungBeetleContentProvider";
	static final boolean DBG = true;
	public static final String SUPER_APP_ID = "edu.stanford.mobisocial.dungbeetle";
    private DBHelper mHelper;
    private IdentityProvider mIdent;

	public DungBeetleContentProvider() {
	}

	@Override
	protected void finalize() throws Throwable {
        mHelper.close();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        final String appId = getCallingActivityId();
        if(appId == null){
            Log.d(TAG, "No AppId for calling activity. Ignoring query.");
            return 0;
        }
        String appSelection = DbObject.APP_ID + "= ?";
        String[] appSelectionArgs = new String[] { appId };
        selection = DBHelper.andClauses(selection, appSelection);
        selectionArgs = DBHelper.andArguments(selectionArgs, appSelectionArgs);
        String[] projection = new String[]  { DbObject.HASH };

        int count = 0;
        Cursor c = mHelper.getReadableDatabase().query(DbObject.TABLE, projection, selection, selectionArgs,
                null, null, null);
        if (c != null && c.moveToFirst()) {
            count = c.getCount();
            long[] hashes = new long[count];
            int i = 0;
            do {
                hashes[i++] = c.getLong(0);
            } while (c.moveToNext());
            Helpers.sendToFeed(getContext(), DeleteObj.from(hashes, true), uri);
        }
		return count;
	}

	@Override
	public String getType(Uri uri) {
		List<String> segs = uri.getPathSegments();
		if (segs.size() == 3){
            return "vnd.android.cursor.item/vnd.dungbeetle.feed";
        }
		else if (segs.size() == 2){
            return "vnd.android.cursor.dir/vnd.dungbeetle.feed";
        }
        else{
			throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
	}

    private Uri uriWithId(Uri uri, long id){
        Uri.Builder b = uri.buildUpon();
        b.appendPath(String.valueOf(id));
        return b.build();
    }

    /**
     * Inserts a message locally that has been received from some agent,
     * typically from a remote device.
     */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
	    ContentResolver resolver = getContext().getContentResolver();
        if (DBG) Log.i(TAG, "Inserting at uri: " + uri + ", " + values);

        final String appId = getCallingActivityId();
        if (appId == null) {
            Log.d(TAG, "No AppId for calling activity. Ignoring query.");
            return null;
        }

        List<String> segs = uri.getPathSegments();
        if (match(uri, "feeds", "me")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }

            long objId = mHelper.addToFeed(appId, "friend", values);
            Uri objUri = DbObject.uriForObj(objId);
            resolver.notifyChange(Feed.uriForName("me"), null);
            resolver.notifyChange(Feed.uriForName("friend"), null);
            resolver.notifyChange(objUri, null);
            return objUri;
        } else if (match(uri, "feeds", "friend", ".+")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            try {
                /*
                 * A "virtual feed" for direct messaging with a friend.
                 * This can be thought of us a sequence of objects "in reply to"
                 * a virtual object between two contacts.
                 */
                /*long timestamp = new Date().getTime();
                String type = values.getAsString(DbObject.TYPE);
                JSONObject json = new JSONObject(values.getAsString(DbObject.JSON));
                mHelper.prepareForSending(json, type, timestamp, appId);
                mHelper.addObjectByJson(Contact.MY_ID, json, new byte[0]);*/
                // stitch, stitch
                long contactId = Long.parseLong(segs.get(2));
                String type = values.getAsString(DbObject.TYPE);
                JSONObject json = new JSONObject(values.getAsString(DbObject.JSON));
                Uri objUri = Helpers.sendMessage(getContext(), contactId, json, type);
                resolver.notifyChange(uri, null);
                return objUri;
            }
            catch(JSONException e){
                return null;
            }
        } else if (match(uri, "feeds", ".+")) {
            String feedName = segs.get(1);
            try {
                JSONObject json = new JSONObject(values.getAsString(DbObject.JSON));
                String objHash = null;
                if (feedName.contains(":")) {
                    String[] parts = feedName.split(":");
                    feedName = parts[0];
                    objHash = parts[1];
                }
                if (objHash != null) {
                    json.put(DbObjects.TARGET_HASH, Long.parseLong(objHash));
                    json.put(DbObjects.TARGET_RELATION, DbRelation.RELATION_PARENT);
                    values.put(DbObject.JSON, json.toString());
                }

                long objId = mHelper.addToFeed(appId, feedName, values);
                Uri objUri = DbObject.uriForObj(objId);
                resolver.notifyChange(objUri, null);
                notifyDependencies(mHelper, resolver, segs.get(1));
                if (DBG) Log.d(TAG, "just inserted " + values.getAsString(DbObject.JSON));
                return objUri;
            }
            catch(JSONException e) {
                return null;
            }
        } else if(match(uri, "out")) {
            try {
                JSONObject obj = new JSONObject(values.getAsString("json"));
                long objId = mHelper.addToOutgoing(appId, values.getAsString(DbObject.DESTINATION),
                        values.getAsString(DbObject.TYPE), obj);
                resolver.notifyChange(Uri.parse(CONTENT_URI + "/out"), null);
                return DbObject.uriForObj(objId);
            }
            catch(JSONException e){
                return null;
            }
        } else if (match(uri, "contacts")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            long id = mHelper.insertContact(values);
            resolver.notifyChange(Uri.parse(CONTENT_URI + "/contacts"), null);
            return uriWithId(uri, id);
        } else if (match(uri, "subscribers")) {
            // Question: Should this be restricted?
            // if(!appId.equals(SUPER_APP_ID)) return null;
            long id = mHelper.insertSubscriber(values);
            resolver.notifyChange(Uri.parse(CONTENT_URI + "/subscribers"), null);
            return uriWithId(uri, id);
        } else if (match(uri, "groups")) {
            if (!appId.equals(SUPER_APP_ID))
                return null;
            long id = mHelper.insertGroup(values);
            getContext().getContentResolver()
                    .notifyChange(Uri.parse(CONTENT_URI + "/groups"), null);
            return uriWithId(uri, id);
        } else if (match(uri, "group_members")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            long id = mHelper.insertGroupMember(values);
            getContext().getContentResolver().notifyChange(
                    Uri.parse(CONTENT_URI + "/group_members"), null);
            getContext().getContentResolver().notifyChange(
                    Uri.parse(CONTENT_URI + "/group_contacts"), null);
            return uriWithId(uri, id);
        }

        else if (match(uri, "group_invitations")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            String groupName = values.getAsString(InviteToGroupObj.GROUP_NAME);
            Uri dynUpdateUri = Uri.parse(values.getAsString(InviteToGroupObj.DYN_UPDATE_URI));
            long gid = values.getAsLong("groupId");
            SQLiteDatabase db = mHelper.getWritableDatabase();
            mHelper.addToOutgoing(db, appId, values.getAsString(InviteToGroupObj.PARTICIPANTS),
                    InviteToGroupObj.TYPE, InviteToGroupObj.json(groupName, dynUpdateUri));
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/out"), null);
            return uriWithId(uri, gid);
        }

        else if (match(uri, "dynamic_groups")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            Uri gUri = Uri.parse(values.getAsString("uri"));
            GroupProviders.GroupProvider gp = GroupProviders.forUri(gUri);
            String feedName = gp.feedName(gUri);
            Maybe<Group> mg = mHelper.groupByFeedName(feedName);
            long id = -1;
            try {
                Group g = mg.get();
                id = g.id;
            } catch (Maybe.NoValError e) {
                ContentValues cv = new ContentValues();
                cv.put(Group.NAME, gp.groupName(gUri));
                cv.put(Group.FEED_NAME, feedName);
                cv.put(Group.DYN_UPDATE_URI, gUri.toString());

                String table = DbObject.TABLE;
                String[] columns = new String[] { DbObject.FEED_NAME };
                String selection = DbObject.CHILD_FEED_NAME + " = ?";
                String[] selectionArgs = new String[] { feedName };
                Cursor parent = mHelper.getReadableDatabase().query(
                        table, columns, selection, selectionArgs, null, null, null);
                try {
	                if (parent.moveToFirst()) {
	                    String parentName = parent.getString(0);
	                    table = Group.TABLE;
	                    columns = new String[] { Group._ID };
	                    selection = Group.FEED_NAME + " = ?";
	                    selectionArgs = new String[] { parentName };
	
	                    Cursor parent2 = mHelper.getReadableDatabase().query(
	                            table, columns, selection, selectionArgs, null, null, null);
	                    try {
		                    if (parent2.moveToFirst()) {
		                        cv.put(Group.PARENT_FEED_ID, parent2.getLong(0));    
		                    } else {
		                        Log.e(TAG, "Parent feed found but no id for " + parentName);
		                    } 
	                    } finally {
	                    	parent2.close();
	                    }
	                } else {
	                    Log.w(TAG, "No parent feed for " + feedName);
	                }
                } finally {
                    parent.close();
                }
                id = mHelper.insertGroup(cv);
                getContext().getContentResolver().notifyChange(
                        Uri.parse(CONTENT_URI + "/dynamic_groups"), null);
                getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/groups"),
                        null);
            }
            return uriWithId(uri, id);
        }

        else if (match(uri, "dynamic_group_member")) {
            if (!appId.equals(SUPER_APP_ID)) {
                return null;
            }
            SQLiteDatabase db = mHelper.getWritableDatabase();
        	db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                String pubKeyStr = values.getAsString(Contact.PUBLIC_KEY);
                RSAPublicKey k = RSACrypto.publicKeyFromString(pubKeyStr);
                String personId = mIdent.personIdForPublicKey(k);
                if (!personId.equals(mIdent.userPersonId())) {
                    cv.put(Contact.PUBLIC_KEY, values.getAsString(Contact.PUBLIC_KEY));
                    cv.put(Contact.NAME, values.getAsString(Contact.NAME));
                    cv.put(Contact.EMAIL, values.getAsString(Contact.EMAIL));
                    if (values.getAsString(Contact.PICTURE) != null) {
                        cv.put(Contact.PICTURE, values.getAsByteArray(Contact.PICTURE));
                    }

                    long cid = -1;
                    Contact contact = mHelper.contactForPersonId(personId).otherwise(Contact.NA());
                    if (contact.id > -1) {
                        cid = contact.id;
                    } else {
                        cid = mHelper.insertContact(db, cv);
                    }

                    if (cid > -1) {

                        ContentValues gv = new ContentValues();
                        gv.put(GroupMember.GLOBAL_CONTACT_ID,
                                values.getAsString(GroupMember.GLOBAL_CONTACT_ID));
                        gv.put(GroupMember.GROUP_ID, values.getAsLong(GroupMember.GROUP_ID));
                        gv.put(GroupMember.CONTACT_ID, cid);
                        mHelper.insertGroupMember(db, gv);
                        getContext().getContentResolver().notifyChange(
                                Uri.parse(CONTENT_URI + "/group_members"), null);
                        getContext().getContentResolver().notifyChange(
                                Uri.parse(CONTENT_URI + "/contacts"), null);
                        getContext().getContentResolver().notifyChange(
                                Uri.parse(CONTENT_URI + "/group_contacts"), null);

                        // Add subscription to this private group feed
                        ContentValues sv = new ContentValues();
                        sv = new ContentValues();
                        sv.put(Subscriber.CONTACT_ID, cid);
                        sv.put(Subscriber.FEED_NAME, values.getAsString(Group.FEED_NAME));
                        mHelper.insertSubscriber(db, sv);

                        ContentValues xv = new ContentValues();
                        xv.put(Subscriber.CONTACT_ID, cid);
                        xv.put(Subscriber.FEED_NAME, "friend");
                        mHelper.insertSubscriber(db, xv);

                        getContext().getContentResolver().notifyChange(
                                Uri.parse(CONTENT_URI + "/subscribers"), null);

                        db.setTransactionSuccessful();
                    }
                    return uriWithId(uri, cid);
                } else {
                    Log.i(TAG, "Omitting self.");
                    return uriWithId(uri, Contact.MY_ID);
                }
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Failed to insert into " + uri);
            return null;
        }
    }

    /*private void restoreDatabase() {
        File data = Environment.getDataDirectory();
        String newDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"+DBHelper.DB_NAME+".new";
        File newDB = new File(data, newDBPath);
        if(newDB.exists()){
    
            String currentDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"+DBHelper.DB_NAME;
            File currentDB = new File(data, currentDBPath);
            currentDB.delete();
            currentDB = new File(data, currentDBPath);
            newDB.renameTo(currentDB);
            
            Log.w(TAG, "backup exists");
        }
        else {
        //database does't exist yet.
            Log.w(TAG, "backup does not exist");
        }


    }*/

    @Override
    public boolean onCreate() {

        
        //restoreDatabase();
    
        Log.i(TAG, "Creating DungBeetleContentProvider");
        mHelper = new DBHelper(getContext());
        mIdent = new DBIdentityProvider(mHelper);
        boolean ok = mHelper.getWritableDatabase() == null;
        if(!ok)
        	return false;
		return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        ContentResolver resolver = getContext().getContentResolver();
        final String realAppId = getCallingActivityId();

        if (realAppId == null) {
            Log.d(TAG, "No AppId for calling activity. Ignoring query.");
            return null;
        }

        if (DBG) Log.d(TAG, "Processing query: " + uri + " from appId " + realAppId);

        List<String> segs = uri.getPathSegments();
        if (match(uri, "obj", ".+")) {
            // objects by database id
            String objId = uri.getLastPathSegment();
            selectionArgs = DBHelper.andArguments(selectionArgs, new String[] { objId });
            selection = DBHelper.andClauses(selection, DbObject._ID + " = ?");
            return mHelper.getReadableDatabase().query(DbObject.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        } else if(match(uri, "obj")) {
            return mHelper.getReadableDatabase().query(DbObject.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        } else if(match(uri, "feedlist")) {
            Cursor c = mHelper.queryFeedList(projection, selection, selectionArgs, sortOrder);
            c.setNotificationUri(resolver, Uri.parse(CONTENT_URI + "/feedlist"));
            return c;
        } else if(match(uri, "feeds", ".+", "head")){
            boolean isMe = segs.get(1).equals("me");
            String feedName = isMe ? "friend" : segs.get(1);
            String select = isMe ? DBHelper.andClauses(
                selection, DbObject.CONTACT_ID + "=" + Contact.MY_ID) : selection;
            Cursor c = mHelper.queryFeedLatest(realAppId, feedName, projection,
                    select, selectionArgs, sortOrder);
            c.setNotificationUri(resolver, Uri.parse(CONTENT_URI + "/feeds/" + feedName));
            if(isMe) c.setNotificationUri(resolver, Uri.parse(CONTENT_URI + "/feeds/me"));
            return c;
        } else if(match(uri, "feeds", ".+")) {
            boolean isMe = segs.get(1).equals("me");
            String feedName = isMe ? "friend" : segs.get(1);
            if (Feed.FEED_NAME_GLOBAL.equals(feedName)) {
                feedName = null;
            }
            String select = isMe ? DBHelper.andClauses(selection, DbObject.CONTACT_ID + "="
                    + Contact.MY_ID) : selection;
            Cursor c = mHelper.queryFeed(realAppId,
                    feedName, projection, select, selectionArgs, sortOrder);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if (match(uri, "feeds", "friend", ".+")) {
            Long contactId = Long.parseLong(segs.get(2));
            String select = selection;
            Cursor c = mHelper.queryFriend(realAppId, contactId, projection,
                    select, selectionArgs, sortOrder);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if(match(uri, "groups_membership", ".+")) {
            if(!realAppId.equals(SUPER_APP_ID)) return null;
            Long contactId = Long.valueOf(segs.get(1));
            Cursor c = mHelper.queryGroupsMembership(contactId);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if(match(uri, "group_contacts", ".+")) {
            if(!realAppId.equals(SUPER_APP_ID)) return null;
            Long group_id = Long.valueOf(segs.get(1));
            Cursor c = mHelper.queryGroupContacts(group_id);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if(match(uri, "local_user", ".+")) {
            // currently available to any local app with a feed id.
            String feed_name = uri.getLastPathSegment();
            Cursor c = mHelper.queryLocalUser(realAppId, feed_name);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if(match(uri, "members", ".+")) {
            if (match(uri, "members", "friend")) {
                // TODO: This is a hack so we can us SocialKit
                // to get the sender of a mass message.
                if(!realAppId.equals(SUPER_APP_ID)) return null;
                return mHelper.getReadableDatabase().query(Contact.TABLE, projection,
                        selection, selectionArgs, null, null, sortOrder);
            }
            String feedName = segs.get(1);
            Cursor c = mHelper.queryFeedMembers(projection, selection, selectionArgs, feedName, realAppId);
            c.setNotificationUri(resolver, uri);
            return c;
        } else if(match(uri, "groups")) {
            if(!realAppId.equals(SUPER_APP_ID)) return null;
            Cursor c = mHelper.queryGroups();
            c.setNotificationUri(resolver, Uri.parse(CONTENT_URI + "/groups"));
            return c;
        } else if(match(uri, "contacts") || 
                match(uri, "subscribers") ||
                match(uri, "group_members")){

            if(!realAppId.equals(SUPER_APP_ID)) return null;

            Cursor c = mHelper.getReadableDatabase().query(
                    segs.get(0), projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(resolver, Uri.parse(CONTENT_URI + "/" + segs.get(0)));
            return c;
        } else if (match(uri, "users"))  {
            if(!realAppId.equals(SUPER_APP_ID)) return null;
            Cursor c = mHelper.getReadableDatabase().query(
                    Contact.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
            return c;
        } else {
            Log.d(TAG, "Unrecognized query: " + uri);
            return null;
        }
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        final String appId = getCallingActivityId();
        if (appId == null) {
            Log.d(TAG, "No AppId for calling activity. Ignoring query.");
            return 0;
        }
        if(!appId.equals(SUPER_APP_ID)) return 0;
        List<String> segs = uri.getPathSegments();

        // TODO: If uri is a feed:
        //String appRestriction = DbObject.APP_ID + "='" + appId + "'";
        //selection = DBHelper.andClauses(selection, appRestriction);

        if (DBG) Log.d(TAG, "Updating uri " + uri + " with " + values);
        mHelper.getWritableDatabase().update(segs.get(0), values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return 0;
    }


// For unit tests
    public DBHelper getDatabaseHelper(){
        return mHelper;
    }

// Helper for matching on url paths
    private boolean match(Uri uri, String... regexes){
        List<String> segs = uri.getPathSegments();
        if(segs.size() == regexes.length){
            for (int i = 0; i < regexes.length; i++) {
                if (!segs.get(i).matches(regexes[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String getCallingActivityId(){
        int pid = Binder.getCallingPid();
        ActivityManager am = (ActivityManager) 
            getContext().getSystemService(Activity.ACTIVITY_SERVICE); 
        List<ActivityManager.RunningAppProcessInfo> lstAppInfo = 
            am.getRunningAppProcesses();

        for(ActivityManager.RunningAppProcessInfo ai : lstAppInfo) { 
            if (ai.pid == pid) {
                return ai.processName;
            } 
        } 
        return null; 
    }

    static void notifyDependencies(DBHelper helper, ContentResolver resolver, String feedName) {
        Uri feedUri = Feed.uriForName(feedName);
        if (DBG) Log.d(TAG, "notifying dependencies of  " + feedUri);
        resolver.notifyChange(feedUri, null);
        resolver.notifyChange(Feed.uriForName(Feed.FEED_NAME_GLOBAL), null);
        if (feedName.contains(":")) {
            feedName = feedName.split(":")[0];
            resolver.notifyChange(Feed.uriForName(feedName), null);
        }
        Cursor c = helper.getFeedDependencies(feedName);
        try {
	        while (c.moveToNext()) {
	            Uri uri = Feed.uriForName(c.getString(0));
	            resolver.notifyChange(uri, null);
	        }
        } finally {
	        c.close();
        }
    }

	public DBHelper getDBHelper() {
		mHelper.addRef();
		return mHelper;
	}
}
