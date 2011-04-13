package edu.stanford.mobisocial.dungbeetle.model;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import java.util.Collection;

public class Group{
    public static final String TABLE = "groups";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String FEED_NAME = "feed_name";
    public static final String DYN_UPDATE_URI = "dyn_update_uri";

    public final String name;
    public final String dynUpdateUri;
    public final Long id;

    public Group(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        dynUpdateUri = c.getString(c.getColumnIndexOrThrow(DYN_UPDATE_URI));
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

}
