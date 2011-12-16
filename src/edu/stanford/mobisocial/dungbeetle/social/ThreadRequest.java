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

package edu.stanford.mobisocial.dungbeetle.social;

import edu.stanford.mobisocial.dungbeetle.HandleGroupSessionActivity;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ThreadRequest {
    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/musubi/thread";

	public static Uri getInvitationUri(Context c, Uri threadUri) {
        return new Uri.Builder()
        	.scheme("http")
        	.authority("mobisocial.stanford.edu")
        	.path("musubi/thread")
        	.appendQueryParameter("uri", threadUri.toString())
        	.build();
	}

	public static void acceptThreadRequest(Context c, Uri uri) {
	    Uri threadUri = Uri.parse(uri.getQueryParameter("uri"));
	    if (threadUri != null && threadUri.getScheme().equals(HomeActivity.GROUP_SESSION_SCHEME)) {
            Intent intent = new Intent().setClass(c, HandleGroupSessionActivity.class);
            intent.setData(threadUri);
            c.startActivity(intent);
	    } else {
	        Toast.makeText(c, "Error joining group.", Toast.LENGTH_SHORT).show();
	    }
	}
}
