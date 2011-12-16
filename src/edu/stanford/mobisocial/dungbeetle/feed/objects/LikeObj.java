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
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class LikeObj extends DbEntryHandler {
    private static final String TAG = "musubi";

    public static final String LABEL = "label";
    public static final String TYPE = "like_ref";

    public static DbObject forObj(Long targetHash) {
        return new DbObject(TYPE, json(targetHash));
    }

    public static DbObject forObj(Long targetHash, String badge) {
        return new DbObject(TYPE, json(targetHash, badge));
    }

    private static JSONObject json(Long targetHash) {
        JSONObject json = new JSONObject();
        try {
            json.put(DbObjects.TARGET_HASH, targetHash);
        } catch (JSONException e) {
        }
        return json;
    }

    private static JSONObject json(Long targetHash, String label) {
        JSONObject json = new JSONObject();
        try {
            json.put(DbObjects.TARGET_HASH, targetHash);
            json.put(LABEL, label);
        } catch (JSONException e) {
        }
        return json;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {

    }
}