package edu.stanford.mobisocial.dungbeetle.model;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import java.util.Collection;

public class MyInfo{
    public static final String TABLE = "my_info";
    public static final String _ID = "_id";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PRIVATE_KEY = "private_key";
    public static final String NAME = "name";
    public static final String EMAIL = "email";

    public final String publicKey;
    public final String privateKey;
    public final String email;
    public final String name;
    public final Long id;

    public MyInfo(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
        publicKey = c.getString(c.getColumnIndexOrThrow(PUBLIC_KEY));
        privateKey = c.getString(c.getColumnIndexOrThrow(PRIVATE_KEY));
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

}
