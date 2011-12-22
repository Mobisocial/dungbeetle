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

package edu.stanford.mobisocial.dungbeetle.feed.objects;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

/**
 * Metadata marking the beginning of a feed.
 */
public class FeedAnchorObj extends DbEntryHandler {
    private static final String TAG = "musubi";
    public static final String TYPE = "feed-anchor";
    public static final String PARENT_FEED_NAME = "parent";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject create(String parentFeedName) {
        return new DbObject(TYPE, json(parentFeedName));
    }

    public static JSONObject json(String parentFeedName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(PARENT_FEED_NAME, parentFeedName);
        } catch (JSONException e) {}
        return obj;
    }

    @Override
    public void afterDbInsertion(Context context, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        String parentFeedName = obj.getJson().optString(PARENT_FEED_NAME);
        if (parentFeedName == null) {
            Log.e(TAG, "anchor for feed, but no parent given");
            return;
        }

        Maybe<Group> parentGroup = Group.forFeedName(context, parentFeedName);
        if (!parentGroup.isKnown()) {
            Log.e(TAG, "No parent entry found for " + parentFeedName);
            return;
        }
        Long parentId = -1l;
        try {
            parentId = parentGroup.get().id;
        } catch (NoValError e) {
        }

        String feedName = feedUri.getLastPathSegment();
        Log.d(TAG, "Updating parent_feed_id for " + feedName);
        DBHelper mHelper = DBHelper.getGlobal(context);
        ContentValues cv = new ContentValues();
        cv.put(Group.PARENT_FEED_ID, parentId);
        mHelper.getWritableDatabase().update(Group.TABLE, cv, Group.FEED_NAME + "=?",
                new String[]{feedName});
        mHelper.close();
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {

    }
}
