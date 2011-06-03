package edu.stanford.mobisocial.dungbeetle.model;
import android.content.Context;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

import java.util.Collection;

public class Group{
    public static final String MIME_TYPE = "vnd.mobisocial.db/group";
    public static final String TABLE = "groups";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String FEED_NAME = "feed_name";
    public static final String DYN_UPDATE_URI = "dyn_update_uri";

    public final String feedName;
    public final String name;
    public final String dynUpdateUri;
    public final Long id;

    public Group(Cursor c){
        this(c.getLong(c.getColumnIndexOrThrow(_ID)),
             c.getString(c.getColumnIndexOrThrow(NAME)),
             c.getString(c.getColumnIndexOrThrow(DYN_UPDATE_URI)),
             c.getString(c.getColumnIndexOrThrow(FEED_NAME))
             );
    }

    public Group(long id, String name, String dynUpdateUri, String feedName){
        this.id = id;
        this.name = name;
        this.dynUpdateUri = dynUpdateUri;
        this.feedName = feedName;
    }

    public static Maybe<Group> forId(Context context, long id) {
        DBHelper helper = new DBHelper(context);
        Maybe<Group> g = helper.groupForGroupId(id);
        helper.close();
        return g;
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

    public static Group NA(){
        return new Group(-1L, "NA", "NA", "NA");
    }

}
