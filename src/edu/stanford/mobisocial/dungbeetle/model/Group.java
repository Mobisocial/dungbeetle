/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.model;
import java.util.Collection;
import java.util.UUID;

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
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class Group{
    // Prefer Feed.MIME_TYPE.
    @Deprecated
    public static final String MIME_TYPE = "vnd.mobisocial.db/group";
    public static final String TABLE = "groups";
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String FEED_NAME = "feed_name";
    public static final String DYN_UPDATE_URI = "dyn_update_uri";
    public static final String VERSION = "version";
    public static final String LAST_UPDATED = "last_updated";
    public static final String LAST_OBJECT_ID = "last_object_id";
    public static final String PARENT_FEED_ID = "parent_feed_id";
    public static final String NUM_UNREAD = "num_unread";
    public static final String GROUP_TYPE = "group_type";

    public static final String TYPE_FRIEND = "friend";
    public static final String TYPE_GROUP = "group";

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
        DBHelper helper = DBHelper.getGlobal(context);
        Maybe<Group> g = helper.groupForGroupId(id);
        helper.close();
        return g;
    }

    public static Maybe<Group> forFeed(Context context, Uri feed) {
        DBHelper helper = DBHelper.getGlobal(context);
        Maybe<Group> g = helper.groupForFeedName(feed.getLastPathSegment());
        helper.close();
        return g;
    }

    public static Maybe<Group> forFeedName(Context context, String feedName) {
        DBHelper helper = DBHelper.getGlobal(context);
        Maybe<Group> g = helper.groupForFeedName(feedName);
        helper.close();
        return g;
    }

    public static Group createForFeed(Context context, String feedName) {
        String groupName = feedName;
        if (feedName.length() > 3) feedName = feedName.substring(0, 3);
        DBHelper helper = DBHelper.getGlobal(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        int version = -1;
        gp.forceUpdate(id, uri, context, version, true);
        ident.close();
        return new Group(id, groupName, uri.toString(), feedName, version);
    }
    
    public void forceUpdate(Context context) {
        DBHelper helper = DBHelper.getGlobal(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        GroupProviders.GroupProvider gp = GroupProviders.forUri(Uri.parse(dynUpdateUri));
        //int version = -1;
        gp.forceUpdate(id, Uri.parse(dynUpdateUri), context, version, false);
        ident.close();

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
        gp.forceUpdate(id, uri, context, version, true);
        ident.close();
        return new Group(id, groupName, uri.toString(), feedName, version);
    }

    public static Group create(Context context) {
        DBHelper helper = DBHelper.getGlobal(context);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String feedName = UUID.randomUUID().toString();
        String groupName = feedName.substring(0, 3);
        Uri uri = GroupProviders.defaultNewSessionUri(ident, groupName, feedName);
        Uri gUri = Helpers.insertGroup(context, groupName, uri.toString(), feedName);
        long id = Long.valueOf(gUri.getLastPathSegment());
        GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
        int version = -1;
        gp.forceUpdate(id, uri, context, version, true);
        helper.close();
        ident.close();
        return new Group(id, groupName, uri.toString(), feedName, version);
    }

    /**
     * Launches an activity to view a group. Deprecated in favor of Feed.view()
     */
    @Deprecated
    public static void view(Context context, Group group) {
        Uri feedUri = Feed.uriForName(group.feedName);
        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setDataAndType(feedUri, Feed.MIME_TYPE);
        context.startActivity(launch);
    }

    public static void join(Context context, Uri dynGroupUri) {
        Uri gUri = Helpers.addDynamicGroup(context, dynGroupUri);

        // Force an immediate update
        long id = Long.valueOf(gUri.getLastPathSegment());
        if(id > -1){
            GroupProviders.GroupProvider gp = GroupProviders.forUri(dynGroupUri);
            DBHelper helper = DBHelper.getGlobal(context);
            DBIdentityProvider ident = new DBIdentityProvider(helper);
            int version = -1;
            gp.forceUpdate(id, dynGroupUri, context, version, true);
            ident.close();
            helper.close();
        }
        try {
            Maybe<Group> group = Group.forId(context, id);
            Group.view(context, group.get());
        } catch (NoValError e) {}
    }
}
