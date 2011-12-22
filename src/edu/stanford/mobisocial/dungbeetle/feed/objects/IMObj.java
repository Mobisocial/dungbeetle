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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;

public class IMObj extends DbEntryHandler implements FeedRenderer {
    public static final String TYPE = "instant_message";
    public static final String TEXT = "text";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(String msg){
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", msg);
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.setComponent(new ComponentName(context.getPackageName(),
                                              HomeActivity.class.getName()));
		PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0,
            launch, PendingIntent.FLAG_CANCEL_CURRENT);
		String msg = obj.optString(TEXT);
		(new PresenceAwareNotify(context)).notify(
            "IM from " + from.name,
            "IM from " + from.name, "\"" + msg + "\"", contentIntent);
	}

	public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
	    JSONObject content = obj.getJson();
        TextView valueTV = new TextView(context);
        valueTV.setText("IM:" + content.optString(TEXT));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
