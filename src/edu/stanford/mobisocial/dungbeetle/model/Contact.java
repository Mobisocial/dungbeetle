package edu.stanford.mobisocial.dungbeetle.model;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.ProfileActivity;
import edu.stanford.mobisocial.dungbeetle.ViewContactTabActivity;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;

import java.util.Date;


public class Contact implements Serializable{

    public static final String TABLE = "contacts";
    public static final long MY_ID = -666;
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

    public final String name;
    public final String email;
    public final String personId;
    public final long id;
    public final long lastPresenceTime;
    public final int presence;
    public final String status;
    public byte[] picture;


    public Contact(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        personId = c.getString(c.getColumnIndexOrThrow(PERSON_ID));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
        presence = c.getInt(c.getColumnIndexOrThrow(PRESENCE));
        lastPresenceTime = c.getLong(c.getColumnIndexOrThrow(LAST_PRESENCE_TIME));
        status = c.getString(c.getColumnIndexOrThrow(STATUS));
        picture = c.getBlob(c.getColumnIndexOrThrow(PICTURE));
    }

    public Contact(Long id, String personId, String name, String email, int presence, long lastPresenceTime, String status){
        this.id = id;
        this.name = name;
        this.email = email;
        this.personId = personId;
        this.presence = presence;
        this.lastPresenceTime = lastPresenceTime;
        this.status = status;
        this.picture = null;
    }

    public static Contact NA(){
        return new Contact(-1L, "NA", "NA", "NA", 1, 0, "NA");
    }

    public static Maybe<Contact> forId(Context context, long id) {
        DBHelper helper = new DBHelper(context);
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

    public static void view(Context context, Long contactId) {
        Intent launch = null;
        
            
        if (contactId == MY_ID) {
            launch = new Intent().setClass(context, ProfileActivity.class);
            launch.putExtra("contact_id", Contact.MY_ID);
        }
        else {
            launch = new Intent(context, ViewContactTabActivity.class);
            launch.putExtra("contact_id", contactId);

            
            try {
                Maybe<Contact> maybeContact = forId(context, contactId);
                launch.putExtra("contact_name", maybeContact.get().name);
            }
            catch (Exception e) {
            }
            
            /*launch = new Intent(Intent.ACTION_VIEW);
            Uri ref = Uri.parse("content://mobisocial.db/contact").buildUpon().appendPath(""+contactId).build();
            launch.setDataAndType(ref, Contact.MIME_TYPE);*/
        }
        context.startActivity(launch);
    }
}
