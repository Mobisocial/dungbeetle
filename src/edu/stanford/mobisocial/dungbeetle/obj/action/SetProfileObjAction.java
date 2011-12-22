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

import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class SetProfileObjAction extends ObjAction {
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        byte[] raw = obj.getRaw();
        JSONObject objData = obj.getJson();
    	if(raw == null) {
	        String b64Bytes = objData.optString(PictureObj.DATA);
	        raw = FastBase64.decode(b64Bytes);
    	}
        Helpers.updatePicture(context, raw);
        Toast.makeText(context, "Set profile picture.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getLabel(Context context) {
        return "Set as Profile";
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }
        return (objType instanceof PictureObj);
    }
}
