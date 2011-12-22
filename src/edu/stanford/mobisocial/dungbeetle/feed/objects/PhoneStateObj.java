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

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class PhoneStateObj extends DbEntryHandler implements FeedRenderer {

    public static final String EXTRA_STATE_UNKNOWN = "UNKNOWN";
    public static final String TYPE = "phone";
    public static final String ACTION = "action";
    public static final String NUMBER = "num";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String action, String number) {
        return new DbObject(TYPE, json(action, number));
    }

    public static JSONObject json(String action, String number){
        JSONObject obj = new JSONObject();
        try{
            obj.put(ACTION, action);
            obj.put(NUMBER, number);
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
        JSONObject content = obj.getJson();
        TextView valueTV = new TextView(context);
        valueTV.setText(asText(content));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    private String asText(JSONObject obj) {
        StringBuilder status = new StringBuilder();
        String a = obj.optString(ACTION);
        String b = obj.optString(NUMBER);
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(a)) {
            status.append("Calling ");
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(a)) {
            status.append("Ending phone call with ");
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(a)) {
            status.append("Inbound call from ");
        }
        status.append(b).append(".");
        return status.toString();
    }

    @Override
    public boolean doNotification(Context context, DbObj obj) {
        return false;
    }
}
