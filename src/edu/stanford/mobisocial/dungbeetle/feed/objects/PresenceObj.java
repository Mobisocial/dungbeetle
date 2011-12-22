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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;


public class PresenceObj extends DbEntryHandler {

    public static final String TYPE = "presence";
    public static final String PRESENCE = "presence";

    public static JSONObject json(int presence){
        JSONObject obj = new JSONObject();
        try{
            obj.put("presence", presence);
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){
        int presence = obj.optInt(PRESENCE);
        String id = Long.toString(from.id);
        long time = obj.optLong(DbObject.TIMESTAMP);
        ContentValues values = new ContentValues();
        values.put(Contact.PRESENCE, presence);
        values.put(Contact.LAST_PRESENCE_TIME, time);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{id});
    }
}