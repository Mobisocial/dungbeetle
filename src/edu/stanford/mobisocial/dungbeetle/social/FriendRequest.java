package edu.stanford.mobisocial.dungbeetle.social;

import java.security.PublicKey;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import android.content.Context;
import android.net.Uri;

public class FriendRequest {
	public static Uri getInvitationUri(Context c) {
        DBHelper helper = new DBHelper(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String name = ident.userName();
        String email = ident.userEmail();
        PublicKey pubKey = ident.userPublicKey();

        Uri.Builder builder = new Uri.Builder()
        	.scheme(DungBeetleActivity.SHARE_SCHEME)
        	.authority("dungbeetle")
        	.appendQueryParameter("name", name)
        	.appendQueryParameter("email", email)
        	.appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey));
        
        helper.close();
        return builder.build();
	}
}
