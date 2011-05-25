package edu.stanford.mobisocial.dungbeetle.social;

import java.security.PublicKey;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import android.content.Context;
import android.net.Uri;

public class FriendRequest {
    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/dungbeetle/join";

	public static Uri getInvitationUri(Context c) {
        DBHelper helper = new DBHelper(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String name = ident.userName();
        String email = ident.userEmail();
        PublicKey pubKey = ident.userPublicKey();
        helper.close();

        return new Uri.Builder()
        	.scheme("http")
        	.authority("mobisocial.stanford.edu")
        	.path("dungbeetle/join")
        	.appendQueryParameter("name", name)
        	.appendQueryParameter("email", email)
        	.appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey))
        	.build();
	}

	public static void acceptFriendRequest(Context c, Uri friendRequest) {
		String name = friendRequest.getQueryParameter("name");
        String email= friendRequest.getQueryParameter("email");
        String pubKeyStr = friendRequest.getQueryParameter("publicKey");
        DBIdentityProvider.publicKeyFromString(pubKeyStr); // may throw exception
        
        Uri uri = Helpers.insertContact(c, 
		                pubKeyStr, name, email);
		long contactId = Long.valueOf(uri.getLastPathSegment());
		Helpers.insertSubscriber(c, contactId, "friend");
		
	}
}
