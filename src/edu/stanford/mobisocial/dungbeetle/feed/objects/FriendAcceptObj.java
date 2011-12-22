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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;

public class FriendAcceptObj extends DbEntryHandler implements UnprocessedMessageHandler {
    public static final String TYPE = "friend_accept";
    public static final String URI = "uri";

    @Override
    public String getType() {
        return TYPE;
    }


    public static DbObject from(Uri uri) {
        return new DbObject(TYPE, json(uri));
    }
    
    public static JSONObject json(Uri uri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(URI, uri.toString());
        } catch(JSONException e) { }
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj) {

    }

    /**
     * Inserts a friend into the list of contacts based on a received
     * DungBeetle message, typically sent in response to peer accepting
     * a friend request.
     */
    @Override
    public Pair<JSONObject, byte[]> handleUnprocessed(Context context, JSONObject msg) {
        Uri uri = Uri.parse(msg.optString(URI));
        // TODO: prompt instead of auto-acccept?
        FriendRequest.acceptFriendRequest(context, uri, true);
        return null;
    }
}
