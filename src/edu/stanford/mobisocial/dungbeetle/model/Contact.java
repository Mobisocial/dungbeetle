package edu.stanford.mobisocial.dungbeetle.model;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.ViewContactActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;


public class Contact implements Serializable{

    public static final String TABLE = "contacts";
    public static final long MY_ID = -666;
    public static final String UNKNOWN = "UNKNOWN";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PERSON_ID = "person_id";
    public static final String EMAIL = "email";
    public static final String PRESENCE = "presence";
    public static final String LAST_PRESENCE_TIME = "last_presence_time";
    public static final String STATUS = "status";
    public static final String PICTURE = "picture";
    public static final String MIME_TYPE = "vnd.mobisocial.db/contact";
	public static final String NEARBY = "nearby";
	public static final String SHARED_SECRET = "secret";
	public static final String LAST_OBJECT_ID = "last_object_id";
	public static final String LAST_UPDATED = "last_updated";
	public static final String NUM_UNREAD = "num_unread";
	public static final String HIDDEN = "hidden";

    public final String name;
    public final String email;
    public final String personId;
    public final long id;
    public final long lastPresenceTime;
    public final int presence;
    public final boolean nearby;
    public final byte[] secret;
    public final String status;
    public Long lastObjectId;
	public Long lastUpdated;
	public long numUnread;
	public int hidden;
	public android.graphics.Bitmap picture;

    // TODO: Move to SocialKit.
    public static final String ATTR_LAN_IP = "vnd.mobisocial.device/lan_ip";

	private static final Set<String> sWellKnownAttrs = new LinkedHashSet<String>();
    static {
        sWellKnownAttrs.add(Contact.ATTR_LAN_IP);
    }

    public static boolean isWellKnownAttribute(String attr) {
        return sWellKnownAttrs.contains(attr);
    }
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    }

    public Contact(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        personId = c.getString(c.getColumnIndexOrThrow(PERSON_ID));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
        presence = c.getInt(c.getColumnIndexOrThrow(PRESENCE));
        lastPresenceTime = c.getLong(c.getColumnIndexOrThrow(LAST_PRESENCE_TIME));
        nearby = c.getInt(c.getColumnIndexOrThrow(NEARBY)) != 0;
        secret = c.getBlob(c.getColumnIndexOrThrow(SHARED_SECRET));
        status = c.getString(c.getColumnIndexOrThrow(STATUS));
        if(!c.isNull(c.getColumnIndexOrThrow(LAST_OBJECT_ID)))
        	lastObjectId = c.getLong(c.getColumnIndexOrThrow(LAST_OBJECT_ID));
        if(!c.isNull(c.getColumnIndexOrThrow(LAST_UPDATED)))
        	lastUpdated = c.getLong(c.getColumnIndexOrThrow(LAST_UPDATED));
        numUnread = c.getLong(c.getColumnIndexOrThrow(NUM_UNREAD));
        byte[] picdata = c.getBlob(c.getColumnIndexOrThrow(PICTURE)); 
        if(picdata != null) {
        	picture = BitmapFactory.decodeByteArray(picdata, 0, picdata.length);
        }
        hidden = c.getInt(c.getColumnIndexOrThrow(HIDDEN));

    }

    public Contact(Long id, String personId, String name, String email, int presence, long lastPresenceTime, boolean nearby, byte[] secret, String status, Long last_object_id, Long last_updated, long num_unread){
        this.id = id;
        this.name = name;
        this.email = email;
        this.personId = personId;
        this.presence = presence;
        this.lastPresenceTime = lastPresenceTime;
        this.nearby = nearby;
        this.secret = secret;
        this.status = status;
        this.lastObjectId = last_object_id;
        this.lastUpdated = last_updated;
        this.numUnread = num_unread;
    }

    public static Contact NA(){
        return new Contact(-1L, "NA", "NA", "NA", 1, 0, false, null, "NA", null, null, 0);
    }

    public static Maybe<Contact> forId(Context context, long id) {
        DBHelper helper = DBHelper.getGlobal(context);
        Maybe<Contact> contact = helper.contactForContactId(id);
        helper.close();
        return contact;
    }

    
    @Override
    public int hashCode(){
        return personId.hashCode();
    }

    @Override
    public boolean equals(java.lang.Object other){
        if(other instanceof Contact){
            Contact c = (Contact)other;
            return c.personId.equals(personId);
        }
        return false;
    }

    public int currentPresenceResource(){
        long t = (new Date()).getTime();
        long idleMins = (long)(((double)(t - lastPresenceTime)) / 60000.0);
        if(presence == Presence.AVAILABLE)// && idleMins < 10) 
            return R.drawable.status_green;
        else if(presence == Presence.AVAILABLE)// && idleMins > 10) 
            return R.drawable.status_yellow;
        else if(presence == Presence.AVAILABLE)// && idleMins > 100) 
            return R.drawable.status_red;
        else if(presence == Presence.BUSY) return R.drawable.status_yellow;
        else if(presence == Presence.AWAY) return R.drawable.status_red;
        else return R.drawable.status_yellow;
    }

    public static void view(Activity foreground, Long contactId) {
        Intent launch = new Intent().setClass(foreground, ViewContactActivity.class);
        launch.putExtra("contact_id", contactId);
        foreground.startActivity(launch);
    }

    public void view(Activity foreground) {
        view(foreground, id);
    }

    public Uri getFeedUri() {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/" + id);
    }

    public static Uri uriFor(long id) {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts/" + id);
    }
}
