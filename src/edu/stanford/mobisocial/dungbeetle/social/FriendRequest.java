
package edu.stanford.mobisocial.dungbeetle.social;

import java.security.PublicKey;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FriendAcceptObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

public class FriendRequest {
    private static final String TAG = "DbFriendRequest";
    private static final boolean DBG = false;

    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/dungbeetle/join";

    public static Uri getInvitationUri(Context c) {
        DBHelper helper = new DBHelper(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        // String name = ident.userName();
        String email = ident.userEmail();
        String profile = "{name:" + ident.userName() + "}";

        PublicKey pubKey = ident.userPublicKey();
        helper.close();

        return new Uri.Builder().scheme("http").authority("mobisocial.stanford.edu")
                .path("dungbeetle/join").appendQueryParameter("profile", profile)
                .appendQueryParameter("email", email)
                .appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey))
                .build();
    }

    public static long acceptFriendRequest(Context c, Uri friendRequest) {
        String email = friendRequest.getQueryParameter("email");
        String name = email;

        try {
            JSONObject o = new JSONObject(friendRequest.getQueryParameter("profile"));
            name = o.getString("name");
            // picture = Base64.decode(o.getString("picture"));
        } catch (Exception e) {
        }

        String pubKeyStr = friendRequest.getQueryParameter("publicKey");
        DBIdentityProvider.publicKeyFromString(pubKeyStr); // may throw
                                                           // exception

        Uri uri = Helpers.insertContact(c, pubKeyStr, name, email);
        long contactId = Long.valueOf(uri.getLastPathSegment());
        Helpers.insertSubscriber(c, contactId, "friend");
        return contactId;
    }

    public static void sendFriendRequest(Context context, long contactId) {
        Maybe<Contact> contactSortOf = Contact.forId(context, contactId);
        try {
            Contact contact = contactSortOf.get();
            Uri uri = getInvitationUri(context);
            DbObject obj = FriendAcceptObj.from(uri);
            Helpers.sendMessage(context, contact, obj);
            if (DBG) Log.d(TAG, "Sent friend request uri " + uri);
        } catch (NoValError e) {
            Log.e(TAG, "Could not locate contact " + contactId);
        }
    }
}
