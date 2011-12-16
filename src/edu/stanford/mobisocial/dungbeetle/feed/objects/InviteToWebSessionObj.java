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

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

public class InviteToWebSessionObj extends DbEntryHandler {

    public static final String TYPE = "invite_web_session";
    public static final String WEB_URL = "webUrl";
    public static final String ARG = "arg";

    @Override
    public String getType() {
        return TYPE;
    }

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		String arg = obj.optString(ARG);
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
		launch.putExtra("creator", false);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String webUrl = obj.optString(WEB_URL);
		launch.setData(Uri.parse(webUrl));
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				launch, PendingIntent.FLAG_CANCEL_CURRENT);
		(new PresenceAwareNotify(context)).notify("New Invitation",
				"Invitation received", "Click to launch application.",
				contentIntent);
	}
}