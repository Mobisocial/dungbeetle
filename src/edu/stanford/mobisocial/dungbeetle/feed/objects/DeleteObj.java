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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * An obj requesting the deletion of some other obj.
 *
 */
public class DeleteObj extends DbEntryHandler {
    private static final String TAG = "dungbeetle";

    public static final String TYPE = "delete";
    public static final String HASH = "hash";
    public static final String HASHES = "hashes";
    /**
     * If true, delete Objs without marking them "deleted".
     */
    public static final String FORCE = "force";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(long hash) {
        return new DbObject(TYPE, json(hash));
    }

    public static DbObject from(long[] hashes, boolean force) {
        return new DbObject(TYPE, json(hashes, force));
    }

    public static JSONObject json(long hash) {
    	//TODO: obj should mention feed
        JSONObject obj = new JSONObject();
        try{
            obj.put(HASH, hash);
        }catch(JSONException e){}
        return obj;
    }

    public static JSONObject json(long[] hashes, boolean force) {
        JSONArray arr = new JSONArray();
        for (long hash : hashes) {
            arr.put(hash);
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put(HASHES, arr);
            obj.put(FORCE, force);
        } catch(JSONException e) {}
        Log.d(TAG, "sending deleteObj " + obj);
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject json) {
        DBHelper dbh = DBHelper.getGlobal(context);
        try {
            long[] hashes;
            if (json.has(HASHES)) {
                JSONArray jsonHashes = json.optJSONArray(HASHES);
                hashes = new long[jsonHashes.length()];
                for (int i = 0; i < jsonHashes.length(); i++) {
                    hashes[i] = jsonHashes.optLong(i);
                }
            } else if (json.has(HASH)) {
                hashes = new long[] { json.optLong(HASH) };
            } else {
                Log.d(TAG, "DeleteObj with no hashes!");
                return;
            }
            dbh.markOrDeleteObjs(hashes);
        } finally {
            dbh.close();
        }
	}

	@Override
	public void afterDbInsertion(Context context, DbObj obj) {
	    Uri feedUri = obj.getContainingFeed().getUri();
		DBHelper dbh = DBHelper.getGlobal(context);
		try {
		    JSONObject json = obj.getJson();
		    long[] hashes;
		    if (json.optJSONArray(HASHES) != null) {
		        JSONArray jsonHashes = json.optJSONArray(HASHES);
		        hashes = new long[jsonHashes.length()];
		        for (int i = 0; i < jsonHashes.length(); i++) {
		            hashes[i] = jsonHashes.optLong(i);
		        }
		    } else if (json.has(HASH)) {
		        hashes = new long[] { json.optLong(HASH) };
		    } else {
		        Log.d(TAG, "DeleteObj with no hashes!");
		        return;
		    }
		    Log.d(TAG, "marking or deleting " + hashes.length);
		    dbh.markOrDeleteFeedObjs(feedUri, hashes,
		            (json.has(FORCE) && json.optBoolean(FORCE)));
		} finally {
			dbh.close();
		}
	}
	@Override
	public boolean discardOutboundObj() {
		return true;
	}
}