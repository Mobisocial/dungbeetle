package edu.stanford.mobisocial.dungbeetle.model;
import java.util.Collection;

import android.content.ContentValues;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;

public class MyInfo {
    public static final String TABLE = "my_info";
    public static final String _ID = "_id";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PRIVATE_KEY = "private_key";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PICTURE = "picture";

    public final String publicKey;
    public final String privateKey;
    public final String email;
    public final String name;
    public final Long id;
    public final byte[] picture;

    public MyInfo(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
        publicKey = c.getString(c.getColumnIndexOrThrow(PUBLIC_KEY));
        privateKey = c.getString(c.getColumnIndexOrThrow(PRIVATE_KEY));
        picture = c.getBlob(c.getColumnIndexOrThrow(PICTURE));
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

    public static void setMyName(DBHelper helper, String name) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.NAME, name);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }

    void setMyEmail(DBHelper helper, String email) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.EMAIL, email);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }
}
