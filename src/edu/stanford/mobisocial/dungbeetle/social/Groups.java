package edu.stanford.mobisocial.dungbeetle.social;

import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.GroupsActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class Groups {

    public static Group createGroup(Context context, String groupName, DBHelper helper) {
        IdentityProvider ident = new DBIdentityProvider(helper);
        String feedName = UUID.randomUUID().toString();
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        gp.forceUpdate(id, uri, context, ident);
        return new Group(id, groupName, uri.toString(), feedName);
    }

    public static Group createGroup(Context context) {
        DBHelper helper = new DBHelper(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String feedName = UUID.randomUUID().toString();
        String groupName = feedName.substring(0, 3);
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        gp.forceUpdate(id, uri, context, ident);
        helper.close();
        return new Group(id, groupName, uri.toString(), feedName);
    }

    public static void startViewActivity(Context context, Group group) {
        Intent launch = new Intent(Intent.ACTION_VIEW);
        Uri ref = Uri.parse("content://mobisocial.db/group").buildUpon().appendPath(""+group.id).build();
        launch.setDataAndType(ref, Group.MIME_TYPE);
        context.startActivity(launch);
    }
}
