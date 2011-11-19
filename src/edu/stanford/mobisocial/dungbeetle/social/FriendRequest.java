
package edu.stanford.mobisocial.dungbeetle.social;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FriendAcceptObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Group.InvalidGroupParameters;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class FriendRequest {
    private static final String TAG = "DbFriendRequest";
    private static final boolean DBG = false;

    public static final String PREFIX_JOIN = "//friend/invite";

    public static Uri getInvitationUri(Context c, String tag) {
    	return getInvitationUri(c, tag, true);
    }

    private static Uri getInvitationUri(Context c, String tag, boolean need_key) {
    	Group g = null;
    	if(need_key) {
        	if(tag == null) {
        		Calendar cal = Calendar.getInstance();
        		tag = "Invitation " + cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + 
        			" " + cal.get(Calendar.DAY_OF_MONTH);
        	}
    		g = Group.create(c, tag);
    		//QQQQQ: mark this group as deleted/not visible/etc
    	}
        DBHelper helper = DBHelper.getGlobal(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        try {
	        // String name = ident.userName();
	        String email = ident.userEmail();
	        String profile = "{name:" + ident.userName() + "}";
	
	        PublicKey pubKey = ident.userPublicKey();
	        helper.close();
	
	        Uri uri = Uri.parse(HomeActivity.SCHEME + ":" + FriendRequest.PREFIX_JOIN);
	        Uri.Builder b = uri.buildUpon();
            b.appendQueryParameter("email", email);
            b.appendQueryParameter("public", DBIdentityProvider.publicKeyToString(pubKey));
            b.appendQueryParameter("profile", profile);
            if(need_key) {
            	b.appendQueryParameter("invite", DBIdentityProvider.publicKeyToString(g.pub));
            	b.appendQueryParameter("key", DBIdentityProvider.privateKeyToString(g.priv));
            }
	        return b.build();
        } finally {
        	ident.close();
        }
    }

    public static long acceptFriendRequest(Context c, Uri friendRequest) {
        String email = friendRequest.getQueryParameter("email");
        String name = email;

        try {
            JSONObject o = new JSONObject(friendRequest.getQueryParameter("profile"));
            name = o.getString("name");
            // picture = FastBase64.decode(o.getString("picture"));
        } catch (Exception e) {
        }

        String pubKeyStr = friendRequest.getQueryParameter("public");
        DBIdentityProvider.publicKeyFromString(pubKeyStr); // may throw
                                                           // exception
        Uri uri = Helpers.insertContact(c, pubKeyStr, name, email);
        long contactId = Long.valueOf(uri.getLastPathSegment());
        Helpers.insertSubscriber(c, contactId, "friend");
        return contactId;
    }

    public static void sendFriendRequest(Context context, long contactId, Uri invitation) {
        Contact contact = Contact.forId(context, contactId);
        if(contact == null) {
            Log.e(TAG, "Could not locate contact " + contactId);
            return;
        }
        DBHelper dbh = DBHelper.getGlobal(context);
        DBIdentityProvider idp = new DBIdentityProvider(dbh);
        try {
	        RSAPrivateKey trusted = DBIdentityProvider.privateKeyFromString(invitation.getQueryParameter("key"));
	        RSAPublicKey trusted_pub = DBIdentityProvider.publicKeyFromString(invitation.getQueryParameter("invite"));
	        Uri uri = getInvitationUri(context, null, false);
	        DbObject obj = FriendAcceptObj.from(uri);
	        List<Contact> cs = new LinkedList<Contact>();
	        cs.add(contact);
	        Helpers.sendMessage(context, cs, obj, trusted, trusted_pub);
	        if (DBG) Log.d(TAG, "Sent friend request uri " + uri);
        } finally {
        	idp.close();
        	dbh.close();
        }
    }
}
