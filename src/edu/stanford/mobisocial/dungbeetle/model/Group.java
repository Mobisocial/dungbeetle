package edu.stanford.mobisocial.dungbeetle.model;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class Group{
	private static final String TAG = "Group";

	// Prefer Feed.MIME_TYPE.
    @Deprecated
    public static final String MIME_TYPE = "vnd.mobisocial.db/group";
    public static final String TABLE = "groups";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String FEED_NAME = "feed_name";
    public static final String DYN_UPDATE_URI = "dyn_update_uri";
    public static final String VERSION = "version";
    public static final String LAST_UPDATED = "last_updated";
    public static final String LAST_OBJECT_ID = "last_object_id";
    public static final String PARENT_FEED_ID = "parent_feed_id";
    public static final String NUM_UNREAD = "num_unread";
	public static final String PUBLIC_KEY = "public_key";
	public static final String PRIVATE_KEY = "private_key";

    public final String feedName;
    public final String name;
    public final String dynUpdateUri;
    public final Long id;
    public final int version;
    public final RSAPublicKey pub;
    public final RSAPrivateKey priv;
    
    static RSAPublicKey createRSAPublicKeyFromByteArray(byte[] d) {
    	if(d == null)
    		return null;
        try {
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(d);
			return (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}                
    }
    static RSAPrivateKey createRSAPrivateKeyFromByteArray(byte[] d) {
    	if(d == null)
    		return null;
        try {
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(d);
	        return (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}                
    }

    public Group(Cursor c){
        this(c.getLong(c.getColumnIndexOrThrow(_ID)),
             c.getString(c.getColumnIndexOrThrow(NAME)),
             c.getString(c.getColumnIndexOrThrow(DYN_UPDATE_URI)),
             c.getString(c.getColumnIndexOrThrow(FEED_NAME)),
             c.getInt(c.getColumnIndexOrThrow(VERSION)),
             createRSAPublicKeyFromByteArray(c.getBlob(c.getColumnIndexOrThrow(PUBLIC_KEY))),
             createRSAPrivateKeyFromByteArray(c.getBlob(c.getColumnIndexOrThrow(PRIVATE_KEY)))
             );
    }

    public Group(long id, String name, String dynUpdateUri, String feedName, int version, RSAPublicKey pub, RSAPrivateKey priv){
        this.id = id;
        this.name = name;
        this.dynUpdateUri = dynUpdateUri;
        this.feedName = feedName;
        this.version = version;
        this.pub = pub;
        this.priv = priv;
    }

    public static Group forId(Context context, long id) {
        DBHelper helper = DBHelper.getGlobal(context);
        Group g = helper.groupForGroupId(id);
        helper.close();
        return g;
    }

    public static Group forFeed(Context context, Uri feed) {
        DBHelper helper = DBHelper.getGlobal(context);
        Group g = helper.groupForFeedName(feed.getLastPathSegment());
        helper.close();
        return g;
    }

    public static Group forFeedName(Context context, String feedName) {
        DBHelper helper = DBHelper.getGlobal(context);
        Group g = helper.groupForFeedName(feedName);
        helper.close();
        return g;
    }
    
    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

    public static Group NA(){
        return new Group(-1L, "NA", "NA", "NA", -1, null, null);
    }

    public static Group create(Context context, String groupName) {
        KeyPair kp = DBIdentityProvider.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey)kp.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey)kp.getPrivate();
		ContentValues cv = new ContentValues();
		cv.put(Group.FEED_NAME, Util.SHA1(pub.getEncoded()));
        cv.put(Group.NAME, groupName);
        cv.put(Group.PUBLIC_KEY, pub.getEncoded());
        cv.put(Group.PRIVATE_KEY, priv.getEncoded());
        Uri gUri = Helpers.insertGroup(context, cv);
        long id = Long.valueOf(gUri.getLastPathSegment());
        return Group.forId(context, id);
    }

    /**
     * Launches an activity to view a group. Deprecated in favor of Feed.view()
     */
    @Deprecated
    public static void view(Context context, Group group) {
        Uri feedUri = Feed.uriForName(group.feedName);
        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setDataAndType(feedUri, Feed.MIME_TYPE);
        context.startActivity(launch);
    }

    public static class InvalidGroupParameters extends Exception {
    	public InvalidGroupParameters(String message) {
    		super(message);
		}
    	public InvalidGroupParameters(String detail, Throwable t) {
    		super(detail, t);
    	}
    }
    /**
     * @param human The human readable name for the group
     * @param members A list of known members of the group, used for the bootstrapping protocol
     * @param name The public key for the group which will be trusted for group update messages
     * @param owner The private key for the group which enables adding friends to the group
     */
    public static Uri makeUriForInvite(final String human, final RSAPublicKey members[], 
    		final RSAPublicKey name, final RSAPrivateKey owner)  throws InvalidGroupParameters {
    	Uri uri = Uri.parse(HomeActivity.SCHEME + ThreadRequest.PREFIX_JOIN);
    	Uri.Builder b = uri.buildUpon();
    	b.appendQueryParameter("human", human);
    	;
    	b.appendQueryParameter("name", DBIdentityProvider.publicKeyToString(name));
    	//some time we may allow people to join groups without them being an owner
    	//this requires that the "ack" from the joiner must go back to one of the members
    	//encoded in the list.  that member would then dispatch the group control messages to
    	//add the new individual to the group
    	//if(owner == null) 
    	//	throw new InvalidGroupParameters("group invitations must include the private key (owner)");
    	if(owner != null)
    		b.appendQueryParameter("owner", DBIdentityProvider.privateKeyToString(owner));
    	if(members == null || members.length == 0)
    		throw new InvalidGroupParameters("group invitations must include at least one known member for the bootstrap protocol");
    	for(int i = 0; i < members.length; ++i) {
    		String param_name = "member" + i;
    		b.appendQueryParameter(param_name, DBIdentityProvider.publicKeyToString(members[i]));
    	}    	
    	return b.build();
    }
    public static class GroupParameters {
    	public String human;
    	public RSAPublicKey members[];
    	public RSAPublicKey name;
    	public RSAPrivateKey owner;
    }
    public static class InvalidGroupUri extends Exception {
    	public InvalidGroupUri(String message) {
    		super(message);
		}
    	public InvalidGroupUri(String detail, Throwable t) {
    		super(detail, t);
    	}
    }
    public static GroupParameters getGroupParameters(Uri uri) throws InvalidGroupUri {
    	GroupParameters gp = new GroupParameters();
    	gp.human = uri.getQueryParameter("human");
    	if(gp.human == null) {
    		gp.human = "**Unknown**";
    	}
    	String name_string = uri.getQueryParameter("name");
    	if(name_string == null)
    		throw new InvalidGroupUri("group uri missing public key (name)");
    	try {
    		gp.name = DBIdentityProvider.publicKeyFromString(name_string);
    	} catch(Exception e) {
    		throw new InvalidGroupUri("group uri had bad public key", e);
    	}
    	String owner_string = uri.getQueryParameter("owner");
    	//it should be legal to not have an owner string.  That means the group is
    	//if(owner_string == null)
    	//	throw new InvalidGroupUri("group uri missing private key (owner)");
    	if(owner_string != null) {
	    	try {
	    		gp.owner = DBIdentityProvider.privateKeyFromString(owner_string);
	    	} catch(Exception e) {
	    		throw new InvalidGroupUri("group uri had bad public key", e);
	    	}
    	}
    	
    	try {
	    	ArrayList<RSAPublicKey> members = new ArrayList<RSAPublicKey>();
	    	for(int i = 0;; ++i) {
	    		String param_name = "member" + i;
	    		String member_string = uri.getQueryParameter(param_name);
	    		if(member_string == null)
	    			break;
	    		members.add(DBIdentityProvider.publicKeyFromString(member_string));
	    	}
	    	//it is possible that there are no members if the local client has just
	    	//created this group
	    	gp.members = members.toArray(new RSAPublicKey[members.size()]);
    	} catch(Exception e) {
    		throw new InvalidGroupUri("group uri had bad member list", e);
    	}
    	return gp;
    }
    public static void join(Context context, Uri invitiation) {
        Uri gUri = Helpers.addDynamicGroup(context, invitiation);
        try {
			GroupParameters gp = getGroupParameters(gUri);
		} catch (InvalidGroupUri e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
			Log.e(TAG, "failed to join group", e);
		}
        long id = Long.valueOf(gUri.getLastPathSegment());
        Group group = Group.forId(context, id);
        if(group != null)
            Group.view(context, group);
    }
}