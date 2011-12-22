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

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

/**
 * Handles Obj content for unknown types.
 *
 */
public class UnknownObj extends DbEntryHandler {

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return "UNKNOWN";
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {   
        Log.w("musubi", "Received unknown obj: " + msg);
    }
}
