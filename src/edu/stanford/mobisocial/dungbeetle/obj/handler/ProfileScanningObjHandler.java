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

package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Iterator;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;

/**
 * Scans inbound objs for user information that can can be added as attributes.
 *
 */
public class ProfileScanningObjHandler implements IObjHandler {
    public static final String TAG = "musubi-profilescanner";
    public static final boolean DBG = true;

    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        JSONObject json = obj.getJson();
        if (DBG) Log.d(TAG, "ProfileScanning obj " + json);
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String attr = iter.next();
            try {
                if (Contact.isWellKnownAttribute(attr)) {
                    if (DBG) Log.d(TAG, "Inserting attribute " + attr + " for " + obj.getSender());
                    String val = json.getString(attr);
                    DbContactAttributes.update(context, obj.getSender().getLocalId(), attr, val);
                }
            } catch (JSONException e) {
                if (DBG) Log.w(TAG, "Could not pull attribute " + attr);
            }
        }
    }

}
