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
import mobisocial.socialkit.SignedObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;


public class InviteToGroupObj extends DbEntryHandler implements FeedRenderer, Activator {
	private static final String TAG = "InviteToGroupObj";
    public static final String TYPE = "invite_group";
    public static final String GROUP_NAME = "groupName";
    public static final String DYN_UPDATE_URI = "dynUpdateUri";
    public static final String PARTICIPANTS = "participants";
    public static final String SENDER = "sender";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(String groupName, Uri dynUpdateUri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(GROUP_NAME, groupName);
            obj.put(DYN_UPDATE_URI, dynUpdateUri.toString());
        }
        catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		try {
			String groupName = obj.getString(GROUP_NAME);
			Uri dynUpdateUri = Uri.parse(obj.getString(DYN_UPDATE_URI));

			Intent launch = new Intent(Intent.ACTION_VIEW);
            launch.setData(dynUpdateUri);
			launch.putExtra("type", TYPE);
			launch.putExtra("creator", false);
			launch.putExtra(SENDER, from.id);
			launch.putExtra(GROUP_NAME, groupName);
			launch.putExtra(DYN_UPDATE_URI, dynUpdateUri);

            PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0,
                launch, 
                PendingIntent.FLAG_CANCEL_CURRENT);

            (new PresenceAwareNotify(context)).notify(
                "Invitation from " + from.name,
                "Invitation from " + from.name, 
                "Join the group '" + groupName + "'.", 
                contentIntent);

		} catch (JSONException e) {
			Log.e(TAG, "Error handling message: ", e);
		}
	}
	@Override
	public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
	    JSONObject content = obj.getJson();

        TextView valueTV = new TextView(context);
        valueTV.setText("Join me in '" +content.optString(GROUP_NAME)+"'");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
	}

	@Override
	public void activate(Context context, SignedObj obj) {
		JSONObject content = obj.getJson();
		String groupName = content.optString(GROUP_NAME);
		Uri dynUpdateUri = Uri.parse(content.optString(DYN_UPDATE_URI));

		Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setData(dynUpdateUri);
		launch.putExtra("type", TYPE);
		launch.putExtra("creator", false);
		launch.putExtra(GROUP_NAME, groupName);
		launch.putExtra(DYN_UPDATE_URI, dynUpdateUri);
		context.startActivity(launch);
	}
}