package edu.stanford.mobisocial.dungbeetle;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.objects.IMObj;
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
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.Util;

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

    public static void deleteGroup(final Context c, 
                                   Long groupId){
        Maybe<Group> mg = Group.forId(c, groupId);
        try{
            Group group = mg.get();
            c.getContentResolver().delete(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + DbObject.TABLE),
                DbObject.FEED_NAME + "=?", new String[]{group.feedName});
            c.getContentResolver().delete(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"),
                Group._ID + "=?", new String[]{ String.valueOf(groupId)});
            c.getContentResolver().delete(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members"),
                GroupMember.GROUP_ID + "=?", new String[]{ String.valueOf(groupId)});
        }
        catch(Exception e) {
        }
    }

    public static void deleteContact(final Context c, 
                                     Long contactId){
        c.getContentResolver().delete(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            Contact._ID + "=?",
            new String[]{ String.valueOf(contactId)});
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

    public static void sendMessage(final Context c,
                                   final Collection<Contact> contacts,
                                   final DbObject obj) {
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        values.put(DbObject.JSON, obj.getJson().toString());
        values.put(DbObject.TYPE, obj.getType());
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
        Maybe<Group> group = Group.forFeed(c, threadUri.toString());
        try {
            sendGroupInvite(c, ids, group.get());
        } catch (NoValError e) {
            Log.e(TAG, "Could not send group invite; no group for " + threadUri, e);
        }
    }

    public static void updatePicture(final Context c, final byte[] data) {
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = ProfilePictureObj.json(data);
        values.put(DbObject.JSON, obj.toString());
        values.put(DbObject.TYPE, ProfilePictureObj.TYPE);
        c.getContentResolver().insert(url, values); 

        values = new ContentValues();
        values.put(MyInfo.PICTURE, data);
        c.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/my_info"),
            values, null, null);

        App.instance().contactImages.invalidate(Contact.MY_ID);
    }
    
    
    /**
     * Sends a message to the default user feed.
     */
    public static void sendToFeed(Context c, ContentValues values) {
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        c.getContentResolver().insert(url, values); 
    }
    
    public static void sendToFeed(Context c, DbObject obj, Uri feed) {
        ContentValues values = new ContentValues();
        values.put(DbObject.JSON, obj.getJson().toString());
        values.put(DbObject.TYPE, obj.getType());
        c.getContentResolver().insert(feed, values); 
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

    public static void updateProfile(final Context c, final String name, final String about){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = ProfileObj.json(name, about);
        values.put(DbObject.JSON, obj.toString());
        values.put(DbObject.TYPE, ProfileObj.TYPE);
        c.getContentResolver().insert(url, values);
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
}
