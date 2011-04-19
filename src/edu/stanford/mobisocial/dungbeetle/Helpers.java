package edu.stanford.mobisocial.dungbeetle;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.objects.SubscribeReqObj;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.content.Context;
import android.database.Cursor;
import java.util.BitSet;
import com.skjegstad.utils.BloomFilter;
import android.util.Base64;
import java.util.ArrayList;

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
        c.getContentResolver().delete(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"),
            Group._ID + "=?", new String[]{ String.valueOf(groupId)});
        c.getContentResolver().delete(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members"),
            GroupMember.GROUP_ID + "=?", new String[]{ String.valueOf(groupId)});
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
                                  String feedNameIn){
        String feedName = feedNameIn;
        if(feedName == null){
            feedName = UUID.randomUUID().toString();
        }
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

    public static BloomFilter getFriendsBloomFilter(final Context c) {
        BloomFilter<String> friendsFilter = new BloomFilter<String>(.001, 1000);
        Cursor cursor = c.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            new String[]{Contact.PUBLIC_KEY}, 
            null, 
            null, 
            null);

        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            String publicKey = cursor.getString(cursor.getColumnIndexOrThrow(Contact.PUBLIC_KEY));
            friendsFilter.add(publicKey);
            cursor.moveToNext();
        }

        return friendsFilter;    
    }

    public static Contact[] checkFriends(final Context c, BloomFilter friendsFilter) {
        Cursor cursor = c.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            null, 
            null, 
            null, 
            null);

        cursor.moveToFirst();

        ArrayList<Contact> friends = new ArrayList<Contact>();
        while(!cursor.isAfterLast()){
            Contact contact = new Contact(cursor);
            String publicKey = cursor.getString(cursor.getColumnIndexOrThrow(Contact.PUBLIC_KEY));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(Contact.NAME));
            if(friendsFilter.contains(publicKey)) {
                Log.w("bloomfilter", name + " is a friend");
                friends.add(contact);
            }
            else {
                Log.w("bloomfilter", name + " is not a friend");
            }
            cursor.moveToNext();
        }

        Contact[] friendsArray = new Contact[friends.size()];
        return friends.toArray(friendsArray);
    }

    public static void sendApplicationInvite(final Context c, 
                                             final Collection<Contact> contacts, 
                                             final String packageName, final String arg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put(InviteObj.PACKAGE_NAME, packageName);
            obj.put(InviteObj.ARG, arg);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, "invite_app_session");
        c.getContentResolver().insert(url, values);
    }

    public static void sendIM(final Context c, 
                              final Collection<Contact> contacts, 
                              final String msg){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", msg);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        Log.i(TAG, "Num contacts: " + contacts.size());
        String to = buildAddresses(contacts);
        Log.i(TAG, "To " + to);
        values.put(Object.DESTINATION, to);
        values.put(Object.TYPE, "instant_message");
        c.getContentResolver().insert(url, values);
    }

    public static void sendFile(final Context c, final Collection<Contact> contacts, 
                                final String mimeType,
                                final String uri){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("mimeType", mimeType);
            obj.put("uri", uri);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.DESTINATION, buildAddresses(contacts));
        values.put(Object.TYPE, "send_file");
        c.getContentResolver().insert(url, values);
    }

    public static void sendAppFeedInvite(Context c, 
                                         Collection<Contact> contacts, 
                                         String feedName,
                                         String packageName){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out");
        ContentValues values = new ContentValues();
        try{
            JSONObject obj = new JSONObject();
            obj.put("packageName", packageName);
            obj.put("sharedFeedName", feedName);
            JSONArray participants = new JSONArray();
            Iterator<Contact> it = contacts.iterator();
            while(it.hasNext()){
                String localId = "@l" + it.next().id;
                participants.put(participants.length(), localId);
            }
            // Need to add ourself to participants
            participants.put(participants.length(), "@l" + Contact.MY_ID);
            obj.put("participants", participants);
            values.put(Object.JSON, obj.toString());
            values.put(Object.DESTINATION, buildAddresses(contacts));
            values.put(Object.TYPE, "invite_app_feed");
            c.getContentResolver().insert(url, values);
        }catch(JSONException e){}
    }

    public static void updateStatus(final Context c, final String feedName, final String status){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName);
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", status);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.TYPE, "status");
        c.getContentResolver().insert(url, values); 
    }

    public static void updatePresence(final Context c, final int presence){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("presence", presence);
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.TYPE, "presence");
        c.getContentResolver().insert(url, values); 
    }

    public static void addDynamicGroup(final Context c, final Uri uri){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/dynamic_groups");
        ContentValues values = new ContentValues();
        values.put("uri", uri.toString());
        c.getContentResolver().insert(url, values);
    }


    /**
     *  Handle a group invite. (user should have approved this action)
     */
    public static void addGroupFromInvite(final Context c,
                                          final String groupName,
                                          final String sharedFeedName,
                                          final long inviterContactId,
                                          final long[] participants
                                          ){
        ContentValues values = new ContentValues();
        values.put("groupName", groupName);
        values.put("sharedFeedName", sharedFeedName);
        values.put("inviterContactId", inviterContactId);
        values.put("participants", Util.join(participants, ","));

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
        values.put("sharedFeedName", g.feedName);
        values.put("groupId", g.id);

        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                            "/group_invitations");
        c.getContentResolver().insert(url, values);
    }

    public static void updateProfile(final Context c, final String name, final String about){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("name", name);
            obj.put("about", about);
            
        }catch(JSONException e){}
        values.put(Object.JSON, obj.toString());
        values.put(Object.TYPE, "profile");
        c.getContentResolver().insert(url, values);
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
