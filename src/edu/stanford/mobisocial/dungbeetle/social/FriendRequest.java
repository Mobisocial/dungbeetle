package edu.stanford.mobisocial.dungbeetle.social;

import java.security.PublicKey;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;
import edu.stanford.mobisocial.bumblebee.util.Base64;

public class FriendRequest {
    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/dungbeetle/join";

	public static Uri getInvitationUri(Context c) {
        DBHelper helper = new DBHelper(c);
        IdentityProvider ident = new DBIdentityProvider(helper);
        //String name = ident.userName();
        String email = ident.userEmail();
        String profile = "{name:"+ident.userName()+"}";
        
        PublicKey pubKey = ident.userPublicKey();
        helper.close();

        return new Uri.Builder()
        	.scheme("http")
        	.authority("mobisocial.stanford.edu")
        	.path("dungbeetle/join")
        	.appendQueryParameter("profile", profile)
        	.appendQueryParameter("email", email)
        	.appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey))
        	.build();
	}

	public static long acceptFriendRequest(Context c, Uri friendRequest) {
	    
        String email= friendRequest.getQueryParameter("email");
        String name = email;
        
        byte[] picture = new byte[0];
        try{
            JSONObject o = new JSONObject(friendRequest.getQueryParameter("profile"));
            name = o.getString("name");
            //picture = Base64.decode(o.getString("picture"));
        }
        catch(Exception e){
        }
        
        String pubKeyStr = friendRequest.getQueryParameter("publicKey");
        DBIdentityProvider.publicKeyFromString(pubKeyStr); // may throw exception
        
        Uri uri = Helpers.insertContact(c, 
		                pubKeyStr, name, email);
		long contactId = Long.valueOf(uri.getLastPathSegment());
		Helpers.insertSubscriber(c, contactId, "friend");
		return contactId;
		
	}
}
