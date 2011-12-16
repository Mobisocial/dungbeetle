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


package edu.stanford.mobisocial.dungbeetle.obj.action;

import mobisocial.socialkit.musubi.DbObj;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.DeleteObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class DeleteAction extends ObjAction {

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        DBHelper dbh = DBHelper.getGlobal(context);
        try {
        	//TODO: do with content provider... this method ignore the 
        	//feed uri for now
            long hash = obj.getHash();
            Uri feedUri = obj.getContainingFeed().getUri();
        	if (hash == 0) {
        		Toast.makeText(context, "Message not yet sent.", Toast.LENGTH_SHORT).show();
        		return;
        	}
        	Helpers.sendToFeeds(context, DeleteObj.TYPE, DeleteObj.json(hash), new Uri[] { feedUri });
        	dbh.deleteObjByHash(feedUri, hash);
        } finally {
        	dbh.close();
        }
    }

    @Override
    public String getLabel(Context context) {
		return "Delete";
    }

}
