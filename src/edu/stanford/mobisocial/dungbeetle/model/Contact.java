package edu.stanford.mobisocial.dungbeetle.model;
import java.io.Serializable;
import android.database.Cursor;

public class Contact implements Serializable{

    public static final String TABLE = "contacts";
    public static final long MY_ID = -666;
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PERSON_ID = "person_id";
    public static final String EMAIL = "email";

    public final String name;
    public final String email;
    public final String personId;
    public final Long id;

    public Contact(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        personId = c.getString(c.getColumnIndexOrThrow(PERSON_ID));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
    }

    public Contact(Long id, String personId, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
        this.personId = personId;
    }


    public static Contact NA(){
        return new Contact(-1L, "NA", "NA", "NA");
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


}
