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

package edu.stanford.mobisocial.dungbeetle.feed.iface;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Base class for object handlers.
 */
public abstract class DbEntryHandler {
    public abstract String getType();

    /**
     * Handle a message that has been received from the network and is
     * intended for the local user directly.
     */
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {
        
    }

    public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
        return objData;
    }

    public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
        return null;
    }

	/**
	 * Handles an object, and returns true to insert it in the database.
	 */
	public boolean handleObjFromNetwork(Context context, Contact contact, JSONObject obj) {
	    return true;
	}

	/**
	 * Executed after an obj has been inserted into the database.
	 */
	public void afterDbInsertion(Context context, DbObj obj) {
	}

	/**
	 * Return true to allow deletions of an obj after it has been sent.
	 */
	public boolean discardOutboundObj() {
	    return false;
	}

	public boolean doNotification(Context context, DbObj obj) {
	    return true;
	}
}