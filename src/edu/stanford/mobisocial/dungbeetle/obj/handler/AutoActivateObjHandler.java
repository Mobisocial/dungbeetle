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

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;

/**
 * Automatically launches some received objects.
 */
public class AutoActivateObjHandler extends ObjHandler {
    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        if (!willActivate(context, obj)) {
            return;
        }
        if (handler instanceof Activator) {
            ((Activator)handler).activate(context, obj);
        }
    }

    public boolean willActivate(Context context, DbObj obj) {
        if (!context.getSharedPreferences("main", 0).getBoolean("autoplay", false)) {
            return false;
        }
        // Don't activate subfeed items
        if (obj.getJson() != null && obj.getJson().has(DbObjects.TARGET_HASH)) {
            return false;
        }
        return true;
    }
}
