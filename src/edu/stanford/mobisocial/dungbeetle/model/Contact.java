package edu.stanford.mobisocial.dungbeetle.model;
import java.io.Serializable;
import android.database.Cursor;

public class Contact implements Serializable{

    public static final String TABLE = "contacts";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PERSON_ID = "person_id";
    public static final String EMAIL = "email";

    public final String name;
    public final String email;
    public final String personId;

    public Contact(Cursor c){
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        personId = c.getString(c.getColumnIndexOrThrow(PERSON_ID));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
    }


    @Override
    public boolean equals(java.lang.Object other){
        if(other instanceof Contact){
            Contact c = (Contact)other;
            return c.name.equals(name) && c.email.equals(email) && c.personId.equals(personId);
        }
        return false;
    }


}
