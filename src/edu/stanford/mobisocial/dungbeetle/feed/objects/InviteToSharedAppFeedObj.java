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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

public class InviteToSharedAppFeedObj extends DbEntryHandler {
	private static final String TAG = "InviteToSharedAppFeedHandler";

    public static final String TYPE = "invite_app_feed";
    public static final String ARG = "arg";
    public static final String PACKAGE_NAME = "packageName";
    public static final String PARTICIPANTS = "participants";
    public static final String FEED_NAME = "feedName";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(Collection<Contact> contacts, 
                                  String feedName,
                                  String packageName){
        JSONObject obj = new JSONObject();
        try{
            obj.put("packageName", packageName);
            obj.put("sharedFeedName", feedName);
            JSONArray participants = new JSONArray();
            Iterator<Contact> it = contacts.iterator();
            while(it.hasNext()){
                String localId = "@l" + it.next().id;
                participants.put(participants.length(), localId);
            }
            // Need to add ourself to participants
            participants.put(participants.length(), "@l" + Contact.MY_ID);
            obj.put("participants", participants);
        }catch(JSONException e){}
        return obj;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		try {
			String packageName = obj.getString(PACKAGE_NAME);
			String feedName = obj.getString("sharedFeedName");
			JSONArray ids = obj.getJSONArray(PARTICIPANTS);
			Intent launch = new Intent();
			launch.setAction(Intent.ACTION_MAIN);
			launch.addCategory(Intent.CATEGORY_LAUNCHER);
			launch.putExtra("type", "invite_app_feed");
			launch.putExtra("creator", false);
			launch.putExtra("sender", from.id);
			launch.putExtra("sharedFeedName", feedName);
			launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			long[] idArray = new long[ids.length()];
			for (int i = 0; i < ids.length(); i++) {
				idArray[i] = ids.getLong(i);
			}
			launch.putExtra("participants", idArray);
			launch.setPackage(packageName);
			final PackageManager mgr = context.getPackageManager();
			List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
			if (resolved.size() == 0) {
				Toast.makeText(
                    context,
                    "Could not find application to handle invite.",
                    Toast.LENGTH_SHORT).show();
				return;
			}
			ActivityInfo info = resolved.get(0).activityInfo;
			launch.setComponent(new ComponentName(info.packageName, info.name));
			PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

			(new PresenceAwareNotify(context)).notify(
                "New Invitation from " + from.name,
                "Invitation received from " + from.name,
                "Click to launch application: " + packageName,
                contentIntent);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}