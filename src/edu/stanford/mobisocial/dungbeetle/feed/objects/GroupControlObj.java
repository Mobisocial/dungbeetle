package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class GroupControlObj extends DbEntryHandler {
	public static final String TAG = "GroupControlObj";
    public static final String TYPE = "groupcontrol";
    public static final String REPLY_TO = "reply_to";
    public static final String KNOWN_MEMBERS = "known_members";
    public static final String NEW_MEMBERS = "new_members";

    @Override
    public String getType() {
        return TYPE;
    }
    //when replying to a group joim you send one of these
    //back to the original guy if you know of members he/she didn't
    public static JSONObject json(Collection<RSAPublicKey> new_members){
        JSONObject obj = new JSONObject();
    	JSONArray json_new_members = new JSONArray();
    	for(RSAPublicKey k : new_members) {
    		json_new_members.put(FastBase64.encode(k.getEncoded()));
    	}
    	try {
			obj.put(NEW_MEMBERS, json_new_members);
		} catch (JSONException e) {
			e.printStackTrace();
		}
        return obj;
    }

    //when you join a group, you send one of these to all of the members you know of
    //after you discover each new member
    public static JSONObject json(RSAPublicKey reply_to, Collection<RSAPublicKey> known_members){
        JSONObject obj = new JSONObject();
    	JSONArray json_known_members = new JSONArray();
    	for(RSAPublicKey k : known_members) {
    		json_known_members.put(Util.SHA1(k.getEncoded()));
    	}
    	try {
    		obj.put(REPLY_TO, FastBase64.encode(reply_to.getEncoded()));
			obj.put(KNOWN_MEMBERS, json_known_members);
		} catch (JSONException e) {
			e.printStackTrace();
		}
        return obj;
    }

	public boolean handleObjFromNetwork(Context context, Contact from, JSONObject obj) {
		DBHelper dbh = DBHelper.getGlobal(context);
		try {
			String feedName = obj.getString("feedName");
			Group g = Group.forFeedName(context, feedName);
			Cursor c = dbh.queryGroupContacts(g.id);
			try {
				Map<String, RSAPublicKey> known_members = new TreeMap<String, RSAPublicKey>();
				if(c.moveToFirst()) do {
					String public_key_string = c.getString(c.getColumnIndexOrThrow("C." +Contact.PUBLIC_KEY));
					RSAPublicKey public_key = DBIdentityProvider.publicKeyFromString(public_key_string);
					known_members.put(Util.SHA1(public_key.getEncoded()), public_key);
				} while(c.moveToNext());

				if(obj.has(REPLY_TO)) {
					RSAPublicKey reply_to = DBIdentityProvider.publicKeyFromString(obj.getString(REPLY_TO));
					String person_id = edu.stanford.mobisocial.bumblebee.util.Util.makePersonIdForPublicKey(reply_to);
					Contact new_member = dbh.contactForPersonId(person_id);
					if(new_member == null) {
				        Uri uri = Helpers.insertContact(context, obj.getString(REPLY_TO), "New Group Member", "new@group.member");
						new_member = dbh.contactForPersonId(person_id);
						if(new_member == null) {
							Toast.makeText(context, "Failure adding friend for group join", Toast.LENGTH_SHORT).show();
							Log.e(TAG, "adding member to friends list failed!");
							return false;
						}
						//since we added this member then they won't have a profile already
			        	LinkedList<Contact> contacts = new LinkedList<Contact>();
			        	contacts.add(new_member);
			        	//we know they have our key, once they get our profile, they will
			        	//send their profile
						Helpers.resendProfile(context, contacts, true);
					}
					//HMM... wtf is the id in group
					Helpers.insertGroupMember(context, g.id, new_member.id, new_member.personId);
					
					//now compare our membership list with theirs
					JSONArray joiner_known_members = obj.optJSONArray(KNOWN_MEMBERS);
					
					//delete the user from the list
					known_members.remove(Util.SHA1(reply_to.getEncoded()));
					//delete all the members they already know about
					if(joiner_known_members != null) {
						for(int i = 0; i < joiner_known_members.length(); ++i) {
							try {
								String public_key_hash_string = joiner_known_members.getString(i);
								known_members.remove(public_key_hash_string);
							} catch(JSONException e) {
								Log.e(TAG, "group control member array parsing error", e);
							}
						}
					}
					//no real ack required
					if(known_members.isEmpty())
						return false;
					ContentValues cv = new ContentValues();
					cv.put(DbObject.JSON, json(known_members.values()).toString());
					cv.put(DbObject.TYPE, TYPE);
					cv.put(DbObject.SEND_AS, DBIdentityProvider.privateKeyToString(g.priv));
					dbh.addToFeed(DungBeetleContentProvider.SUPER_APP_ID, g.feedName, cv);
				} else {
					
				}
			} finally {
				c.close();
			}
		} catch(JSONException e) {
			Log.e(TAG, "failed to handle group control message", e);
		} finally {
			dbh.close();
		}
		//don't save it
        return false;
	}

	@Override
	public boolean discardOutboundObj() {
		return true;
	};
}
