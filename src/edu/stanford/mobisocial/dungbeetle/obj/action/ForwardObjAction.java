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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

public class ForwardObjAction extends ObjAction {
    private static final String TAG = "passItOn";
    private static JSONObject mJson;
    private static DbEntryHandler mType;
    private Context mContext;

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        mContext = context;
        JSONObject objData = obj.getJson();
        byte[] raw = obj.getRaw();
    	objData = objType.mergeRaw(objData, raw);
        holdObj(context, objType, objData);
        ((InstrumentedActivity)context).doActivityForResult(mTargetSelected);
    }

    @Override
    public String getLabel(Context context) {
        return "Share";
    }

    //Helpers.sendToFeed(context, new DbObject(mType, mJson), feedUri);

    public static void holdObj(Context context, DbEntryHandler type, JSONObject json) {
        mType = type;
        mJson = json;

        // TODO: Expand support by allowing to "paste" to another app
        // in the picker.
        /*
        if (json.has(StatusObj.TEXT)) {
            ClipboardManager m = (ClipboardManager)context.getSystemService(
                    Context.CLIPBOARD_SERVICE);
            m.setText(json.optString(StatusObj.TEXT));
        }*/
    }

    private ActivityCallout mTargetSelected = new ActivityCallout() {
        @Override
        public void handleResult(int resultCode, Intent data) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            Parcelable[] feedParc = (Parcelable[])data.getParcelableArrayExtra(PickContactsActivity.EXTRA_FEEDS);
            Uri[] uris = new Uri[feedParc.length];
            int i = 0;
            for (Parcelable p : feedParc) {
                uris[i++] = (Uri)p;
            }
            Helpers.sendToFeeds(mContext, mType.getType(), mJson, uris);
        }
        
        @Override
        public Intent getStartIntent() {
            // TODO:
            //return new Intent(Intent.ACTION_PICK, Contact.MIME_TYPE);
            Intent picker = new Intent(mContext, PickContactsActivity.class);
            picker.setAction(Intent.ACTION_PICK);
            return picker;
        }
    };

    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        mContext = context;
        return MusubiBaseActivity.isDeveloperModeEnabled(context);
    }
}
