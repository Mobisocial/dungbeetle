/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.IMObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToGroupObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.Util;

/**
 * A grab bag of utility methods. Avoid adding new code here.
 *
 */
public class Helpers {
    public static final String TAG = "Helpers";

    public static void insertSubscriber(final Context c, 
                                        Long contactId, 
                                        String feedName){
        ContentValues values = new ContentValues();
        values.put(Subscriber.CONTACT_ID, contactId);
        values.put("feed_name", feedName);
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/subscribers");
        c.getContentResolver().insert(url, values);
    }

   

    public static void deleteContact(final Context c, 
                                     Long contactId){
        /*c.getContentResolver().delete(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            Contact._ID + "=?",
            new String[]{ String.valueOf(contactId)});
        */

        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        ContentValues values = new ContentValues();
        values.put(Contact.HIDDEN, 1);
        c.getContentResolver().update(url, values, Contact._ID + "=" + contactId, null);
    }

    public static Uri insertContact(final Context c, String pubKeyStr, 
                                    String name, String email){
        ContentValues values = new ContentValues();
        values.put(Contact.PUBLIC_KEY, pubKeyStr);
        values.put(Contact.NAME, name);
        values.put(Contact.EMAIL, email);
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        return c.getContentResolver().insert(url, values);
    }

    public static Uri insertGroup(final Context c, 
                                  String groupName, 
                                  String dynUpdateUri, 
                                  String feedName){
        assert (groupName != null && dynUpdateUri != null && feedName != null);
        ContentValues values = new ContentValues();
        values.put(Group.NAME, groupName);
        values.put(Group.DYN_UPDATE_URI, dynUpdateUri);
        values.put(Group.FEED_NAME, feedName);
        return c.getContentResolver().insert(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), values);
    }

    public static void insertGroupMember(final Context c, 
                                         final long groupId, 
                                         final long contactId, 
                                         final String idInGroup){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members");
        ContentValues values = new ContentValues();
        values.put(GroupMember.GROUP_ID, groupId);
        values.put(GroupMember.CONTACT_ID, contactId);
        values.put(GroupMember.GLOBAL_CONTACT_ID, idInGroup);
        c.getContentResolver().insert(url, values);
    }

    public static void updateGroupVersion(final Context c,
                                   final long groupId,
                                   final int version){
       Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups");
       ContentValues values = new ContentValues();
       values.put(Group.VERSION, version);
       c.getContentResolver().update(url, values, Group._ID + "=" + groupId, null);
   }

    /**
     * @see Helpers#sendMessage(Context, Collection, DbObject)
     */
    @Deprecated
    public static void sendIM(final Context c, 
                              final Collection<Contact> contacts, 
                              final String msg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = IMObj.json(msg);
        values.put(DbObject.JSON, obj.toString());
        String to = buildAddresses(contacts);
        values.put(DbObject.DESTINATION, to);
        values.put(DbObject.TYPE, IMObj.TYPE);
        c.getContentResolver().insert(url, values);
    }

    @Deprecated
    public static Uri sendMessage(final Context c, long contactId, JSONObject json, String type) {
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        Obj obj = DbObjects.convertOldJsonToObj(c, type, json);
        ContentValues values = new ContentValues();
        values.put(DbObject.TYPE, type);
        values.put(DbObject.JSON, obj.getJson().toString());
        if (obj.getRaw() != null) {
            values.put(DbObject.RAW, obj.getRaw());
        }
        String to = Long.toString(contactId);
        values.put(DbObject.DESTINATION, to);
        return c.getContentResolver().insert(url, values);
    }

    @Deprecated
    public static void sendMessage(final Context c,
                                   final Collection<Contact> contacts,
                                   final DbObject obj) {
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        values.put(DbObject.JSON, obj.getJson().toString());
        values.put(DbObject.TYPE, obj.getType());
        byte[] raw = obj.getRaw();
        if (raw != null) {
            values.put(DbObject.RAW, raw);
        }
        String to = buildAddresses(contacts);
        values.put(DbObject.DESTINATION, to);
        c.getContentResolver().insert(url, values);
    }

    public static void sendMessage(final Context context,
            final Contact contact,
            final DbObject obj) {
        sendMessage(context, Collections.singletonList(contact), obj);
    }

    public static void sendAppFeedInvite(Context c, 
                                         Collection<Contact> contacts, 
                                         String feedName,
                                         String packageName){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = InviteToSharedAppFeedObj.json(contacts, feedName, packageName);
        values.put(DbObject.JSON, obj.toString());
        values.put(DbObject.DESTINATION, buildAddresses(contacts));
        values.put(DbObject.TYPE, InviteToSharedAppFeedObj.TYPE);
        c.getContentResolver().insert(url, values);
    }

    public static void sendThreadInvite(Context c, Collection<Contact> contacts, Uri threadUri) {
        long[] ids = new long[contacts.size()];
        Iterator<Contact> it = contacts.iterator();
        int i = 0;
        while(it.hasNext()){
            Contact me = it.next();
            ids[i] = me.id;
            i++;
        }
        Maybe<Group> group = Group.forFeed(c, threadUri);
        try {
            sendGroupInvite(c, ids, group.get());
        } catch (NoValError e) {
            Log.e(TAG, "Could not send group invite; no group for " + threadUri, e);
        }
    }

    /**
     * @see Helpers#sendToFeed(Context, Obj, Uri)
     */
    @Deprecated
    public static Uri sendToFeed(Context c, DbObject obj, Uri feed) {
        ContentValues values = new ContentValues();

        values.put(DbObject.TYPE, obj.getType());
        DbEntryHandler objHandler = DbObjects.forType(obj.getType());
        if (objHandler instanceof UnprocessedMessageHandler) {
            Pair<JSONObject, byte[]> r = ((UnprocessedMessageHandler)objHandler)
                    .handleUnprocessed(c, obj.getJson());
            if(r != null) {
                values.put(DbObject.JSON, r.first.toString());
                values.put(DbObject.RAW, r.second);
            } else {
                values.put(DbObject.JSON, obj.getJson().toString());
                values.put(DbObject.RAW, obj.getRaw());
            }
        } else {
            values.put(DbObject.JSON, obj.getJson().toString());
        }
        return c.getContentResolver().insert(feed, values);
    }

    public static Uri sendToFeed(Context c, Obj obj, Uri feed) {
        ContentValues values = new ContentValues();
        values.put(DbObject.TYPE, obj.getType());
        Object value = obj.getJson().toString();
        if (value != null) {
            values.put(DbObject.JSON, (String)value);
        }
        value = obj.getRaw();
        if (value != null) {
            values.put(DbObject.RAW, (byte[])value);
        }
        return c.getContentResolver().insert(feed, values);
    }

    /**
     * A convenience method for sending an object to multiple feeds.
     * TODO: This should be made much more efficient if it proves useful.
     */
    public static void sendToFeeds(Context c, DbObject obj, Collection<Uri> feeds) {
        for (Uri feed : feeds) {
            sendToFeed(c, obj, feed);
        }
    }

    /**
     * A convenience method for sending an object to multiple feeds.
     * TODO: This should be made much more efficient if it proves useful.
     */
    public static void sendToFeeds(Context c, String type, JSONObject json, Uri[] feeds) {
        Obj obj = DbObjects.convertOldJsonToObj(c, type, json);
        for (Uri feed : feeds) {
            sendToFeed(c, obj, feed);
        }
    }

    public static void updatePresence(final Context c, final int presence){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = PresenceObj.json(presence);
        values.put(DbObject.JSON, obj.toString());
        values.put(DbObject.TYPE, PresenceObj.TYPE);
        c.getContentResolver().insert(url, values); 
    }

    public static Uri addDynamicGroup(final Context c, final Uri uri){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/dynamic_groups");
        ContentValues values = new ContentValues();
        values.put("uri", uri.toString());
        return c.getContentResolver().insert(url, values);
    }


/**
 *  Handle a group invite. (user should have approved this action)
 */
    public static void addGroupFromInvite(final Context c,
                                          final String groupName,
                                          final String sharedFeedName,
                                          final long inviterContactId,
                                          final String dynUpdateUri
                                          ){
        ContentValues values = new ContentValues();
        values.put("groupName", groupName);
        values.put("sharedFeedName", sharedFeedName);
        values.put("inviterContactId", inviterContactId);
        values.put("inviterContactId", inviterContactId);
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                            "/groups_by_invitation");
        c.getContentResolver().insert(url, values);
    }


/**
 *  Add contacts to the group g. Send an invite to each contact
 *  to join the private group feed.
 */
    public static void sendGroupInvite(final Context c,
                                       final long[] participants,
                                       final Group g){

        ContentValues values = new ContentValues();
        values.put("participants", Util.join(participants, ","));
        values.put("groupName", g.name);
        values.put("dynUpdateUri", g.dynUpdateUri);
        values.put("groupId", g.id);

        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                            "/group_invitations");
        c.getContentResolver().insert(url, values);
    }

    public static void sendGroupInvite(final Context c, Uri feed,
            final String group_name, Uri updateUri) {

        ContentValues cv = new ContentValues();
        cv.put(DbObject.JSON, InviteToGroupObj.json(group_name, updateUri).toString());
        cv.put(DbObject.TYPE, InviteToGroupObj.TYPE);
        c.getContentResolver().insert(feed, cv); 
	}

    public static void resendProfile(final Context c, final Collection<Contact> contacts, final boolean reply) {
    	if (contacts.isEmpty()) {
    		return;
    	}
        DBHelper helper = DBHelper.getGlobal(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        Log.w(TAG, "attempting to resend");
        try {
            JSONObject profileJson = new JSONObject(ident.userProfile());
            //updateProfile(c, profileJson.optString("name"), "");
            //updatePicture(c, FastBase64.decode(profileJson.optString("picture")));
            sendMessage(c, contacts, new DbObject(ProfileObj.TYPE, ProfileObj.json(profileJson.optString("name"), "")));
            Log.w(TAG, "string: " + profileJson.optString("picture"));
            sendMessage(c, contacts, new DbObject(ProfilePictureObj.TYPE, ProfilePictureObj.json(FastBase64.decode(profileJson.optString("picture")), reply)));
            
            Log.w(TAG, "resending profile");
        }
        catch (Exception e) {
        }
        ident.close();
    }

    public static void sendToEveryone(final Context c, Obj obj){
        Uri uri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = DbObj.toContentValues(obj);
        c.getContentResolver().insert(uri, values);
    }
    

    public static void updatePicture(final Context c, final byte[] data) {
    	//fragments cause this
    	if(c == null)
    		return;
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = ProfilePictureObj.json(data, false);
        values.put(DbObject.JSON, obj.toString());
        values.put(DbObject.TYPE, ProfilePictureObj.TYPE);
        c.getContentResolver().insert(url, values); 

        values = new ContentValues();
        values.put(MyInfo.PICTURE, data);
        c.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/my_info"),
            values, null, null);

    	//todo: should be below content provider... but then all of dbidentityprovider is like this
		DBHelper dbh = new DBHelper(c);
		try {
	    	MyInfo.setMyPicture(dbh, data);
		} finally {
			dbh.close();
		}
		Helpers.invalidateContacts();
    }

    public static void updateLastPresence(final Context c, 
                                          final Contact contact, 
                                          final long time){
        ContentValues values = new ContentValues();
        values.put(Contact.LAST_PRESENCE_TIME, time);
        c.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{ String.valueOf(contact.id) });
    }

    private static String buildAddresses(Collection<Contact> contacts){
        String to = "";
        Iterator<Contact> it = contacts.iterator();
        while(it.hasNext()){
            Contact c = it.next();
            if(it.hasNext()){
                to += c.id + ",";
            }
            else{
                to += c.id;
            }
        }
        return to;
    }

    private static HashMap<Long, SoftReference<Contact>> g_contacts = new HashMap<Long, SoftReference<Contact>>();
    public static void invalidateContacts() {
    	g_contacts.clear();
    }
    public static Contact getContact(Context context, long contactId) {
    	SoftReference<Contact> entry = g_contacts.get(contactId);
    	if(entry != null) {
	    	Contact c = entry.get();
	    	if(c != null)
	    		return c;
    	}
    	Contact c = forceGetContact(context, contactId);
    	g_contacts.put(contactId, new SoftReference<Contact>(c));
    	return c;
    }
	public static Contact forceGetContact(Context context, long contactId) {
		if(contactId == Contact.MY_ID) {
			DBHelper dbh = new DBHelper(context);
			DBIdentityProvider idp = new DBIdentityProvider(dbh);
			try {
				return idp.contactForUser();
			} finally {
				idp.close();
				dbh.close();
			}
		}
        Cursor c = context.getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), null,
                Contact._ID + "=?", new String[] {
                    String.valueOf(contactId)
                }, null);
        if(c == null)
        	return null;
        try {
            
            if (!c.moveToFirst()) {
                return null;
            } else {
                return new Contact(c);
            }
        } finally {
        	c.close();
        }
	}
}
