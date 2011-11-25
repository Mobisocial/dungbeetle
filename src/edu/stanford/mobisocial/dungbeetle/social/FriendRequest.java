
package edu.stanford.mobisocial.dungbeetle.social;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.UUID;

import edu.stanford.mobisocial.bumblebee.util.Util;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FriendAcceptObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import mobisocial.socialkit.musubi.RSACrypto;

import org.json.JSONObject;

public class FriendRequest {
    private static final String TAG = "DbFriendRequest";
    private static final boolean DBG = true;
    public static final String PREF_FRIEND_CAPABILITY = "friend.cap";

    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/musubi/join";

    /**
     * Returns a friending uri under the 'musubi' scheme.
     */
    public static Uri getMusubiUri(Context c) {
        Uri uri = getInvitationUri(c, true);
        return uri.buildUpon().scheme("musubi").build();
    }

    public static Uri getInvitationUri(Context c) {
        return getInvitationUri(c, true);
    }

    private static Uri getInvitationUri(Context c, boolean appendCapability) {
        SharedPreferences p = c.getSharedPreferences("main", 0);
        String cap = p.getString(PREF_FRIEND_CAPABILITY, null);
        if (cap == null) {
            String capability = App.instance().getRandomString();
            p.edit().putString(PREF_FRIEND_CAPABILITY, capability).commit();
            cap = capability;
        }
        DBHelper helper = DBHelper.getGlobal(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        try {
	        // String name = ident.userName();
	        String email = ident.userEmail();
	        String name = ident.userName();
	
	        PublicKey pubKey = ident.userPublicKey();
	        helper.close();
	
	        Uri.Builder builder = new Uri.Builder().scheme("http").authority("mobisocial.stanford.edu")
	                .path("musubi/join").appendQueryParameter("name", name)
	                .appendQueryParameter("email", email)
	                .appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey));
	        if (appendCapability) {
	            builder.appendQueryParameter("cap", cap);
	        }
	        return builder.build();
        } finally {
        	ident.close();
        }
    }

    /**
     * Returns the contact id for the contact associated with the given uri,
     * or -1 if no such contact exists.
     */
    public static long getExistingContactId(Context context, Uri friendRequest) {
        String personId = null;
        try {
            String pubKeyStr = friendRequest.getQueryParameter("publicKey");
            PublicKey key = RSACrypto.publicKeyFromString(pubKeyStr);
            personId = Util.makePersonIdForPublicKey(key);
        } catch (Exception e) {
            return -1;
        }
        Uri uri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        String[] projection = new String[] { Contact._ID };
        String selection = Contact.PERSON_ID + " = ?";
        String[] selectionArgs = new String[] { personId };
        String sortOrder = null;
        Cursor c = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        if (!c.moveToFirst()) {
            return -1;
        }
        return c.getLong(0);
    }

    public static long acceptFriendRequest(Context c, Uri friendRequest, boolean requireCapability) {
        String email = friendRequest.getQueryParameter("email");
        String name = friendRequest.getQueryParameter("name");
        if (name == null) {
            name = email;
        }
        

        String pubKeyStr = friendRequest.getQueryParameter("publicKey");
        RSACrypto.publicKeyFromString(pubKeyStr); // may throw exception
        String cap = friendRequest.getQueryParameter("cap");
        if (requireCapability) {
            if (cap == null) {
                Log.w(TAG, "Unapproved friend request");
                return -1;
            }
            SharedPreferences p = c.getSharedPreferences("main", 0);
            String myCap = p.getString(PREF_FRIEND_CAPABILITY, null);
            if (myCap == null) {
                Log.w(TAG, "No capability available");
                return -1;
            }
            if (!cap.equals(myCap)) {
                Log.w(TAG, "Capability mismatch");
                return -1;
            }
        }

        Uri uri = Helpers.insertContact(c, pubKeyStr, name, email);
        long contactId = Long.valueOf(uri.getLastPathSegment());
        Helpers.insertSubscriber(c, contactId, "friend");
        return contactId;
    }

    public static void sendFriendRequest(Context context, long contactId, String capability) {
        Maybe<Contact> contactSortOf = Contact.forId(context, contactId);
        try {
            Contact contact = contactSortOf.get();
            Uri uri = getInvitationUri(context, false);
            if (capability != null) {
                uri = uri.buildUpon().appendQueryParameter("cap", capability).build();
            }
            DbObject obj = FriendAcceptObj.from(uri);
            Helpers.sendMessage(context, contact, obj);
            if (DBG) Log.d(TAG, "Sent friend request uri " + uri);
        } catch (NoValError e) {
            Log.e(TAG, "Could not locate contact " + contactId);
        }
    }
}
