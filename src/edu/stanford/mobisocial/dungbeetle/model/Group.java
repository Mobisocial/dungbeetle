package edu.stanford.mobisocial.dungbeetle.model;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

import java.util.Collection;
import java.util.UUID;

public class Group{
    public static final String MIME_TYPE = "vnd.mobisocial.db/group";
    public static final String TABLE = "groups";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String FEED_NAME = "feed_name";
    public static final String DYN_UPDATE_URI = "dyn_update_uri";
    public static final String VERSION = "version";

    public final String feedName;
    public final String name;
    public final String dynUpdateUri;
    public final Long id;
    public final int version;

    public Group(Cursor c){
        this(c.getLong(c.getColumnIndexOrThrow(_ID)),
             c.getString(c.getColumnIndexOrThrow(NAME)),
             c.getString(c.getColumnIndexOrThrow(DYN_UPDATE_URI)),
             c.getString(c.getColumnIndexOrThrow(FEED_NAME)),
             c.getInt(c.getColumnIndexOrThrow(VERSION))
             );
    }

    public Group(long id, String name, String dynUpdateUri, String feedName, int version){
        this.id = id;
        this.name = name;
        this.dynUpdateUri = dynUpdateUri;
        this.feedName = feedName;
        this.version = version;
    }

    public static Maybe<Group> forId(Context context, long id) {
        DBHelper helper = new DBHelper(context);
        Maybe<Group> g = helper.groupForGroupId(id);
        helper.close();
        return g;
    }

    public static Maybe<Group> forFeed(Context context, String feed) {
        DBHelper helper = new DBHelper(context);
        Maybe<Group> g = helper.groupForFeedName(feed);
        helper.close();
        return g;
    }

    public static Group createForFeed(Context context, String feedName) {
        String groupName = feedName;
        if (feedName.length() > 3) feedName = feedName.substring(0, 3);
        DBHelper helper = new DBHelper(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        int version = -1;
        gp.forceUpdate(id, uri, context, ident, version);
        return new Group(id, groupName, uri.toString(), feedName, version);
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

    public static Group NA(){
        return new Group(-1L, "NA", "NA", "NA", -1);
    }

    public static Group create(Context context, String groupName, DBHelper helper) {
        IdentityProvider ident = new DBIdentityProvider(helper);
        String feedName = UUID.randomUUID().toString();
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        int version = -1;
        gp.forceUpdate(id, uri, context, ident, version);
        return new Group(id, groupName, uri.toString(), feedName, version);
    }

    public static Group create(Context context) {
        DBHelper helper = new DBHelper(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String feedName = UUID.randomUUID().toString();
        String groupName = feedName.substring(0, 3);
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        int version = -1;
        gp.forceUpdate(id, uri, context, ident, version);
        helper.close();
        return new Group(id, groupName, uri.toString(), feedName, version);
    }

    public static void view(Context context, Group group) {
        Intent launch = new Intent(Intent.ACTION_VIEW);
        Uri ref = Uri.parse("content://mobisocial.db/group").buildUpon().appendPath(""+group.id).build();
        launch.setDataAndType(ref, Group.MIME_TYPE);
        context.startActivity(launch);
    }
}
