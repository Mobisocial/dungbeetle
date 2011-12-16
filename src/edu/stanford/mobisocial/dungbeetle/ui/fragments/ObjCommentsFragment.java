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

package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import org.json.JSONObject;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.Gallery;
import android.widget.SpinnerAdapter;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.adapter.ObjectListCursorAdapter;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

/**
 * Retreives a cursor from the Content Provider by adding selection constraints:
 * 
 * Musubi notation:
 * --        obj_id = '5098d876effd4af6d7fa0ddf'
 *       ref_obj_id = '2dbe76f6f5df7d6dbf9da765'
 *       feed       = '9fedf7eaaef7cce45deac77a'
 * and relationship = 'child'
 * and         type = 'comment'
 * 
 * EbXML notation:
 *      ref_obj1_id = '5098d876effd4af6d7fa0ddf'
 *      ref_obj2_id = '2dbe76f6f5df7d6dbf9da765'
 * and relationship = 'child'
 * and         type = 'comment'
 */
public class ObjCommentsFragment {

    /**
     * The parametrization here is absolutely not final.
     */
    public static View getViewForObjComments(Activity activity, Uri feedUri, JSONObject obj) {

        Cursor c = activity.getContentResolver().query(feedUri, null,
                DbObjects.getFeedObjectClause(null), null, DbObject._ID + " DESC LIMIT 2");
        try {
	        SpinnerAdapter adapter = new ObjectListCursorAdapter(activity, c);
	
	        Gallery gallery = new Gallery(activity);
	        gallery.setLayoutParams(CommonLayouts.FULL_SCREEN);
	        gallery.setAdapter(adapter);
	
	        return gallery;
        } finally {
        	c.close();
        }
    }
}
