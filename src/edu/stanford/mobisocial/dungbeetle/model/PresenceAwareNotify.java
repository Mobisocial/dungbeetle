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

package edu.stanford.mobisocial.dungbeetle.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class PresenceAwareNotify {
    private static final String TAG = "musubi";
	public static final int NOTIFY_ID = 9847184;
	private NotificationManager mNotificationManager;
	private final long[] VIBRATE = new long[] {0, 250, 80, 100, 80, 80, 80, 250};
	Context mContext;

    public static final String PREFS_NAME = "DungBeetlePrefsFile";

    public PresenceAwareNotify(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
        
    public void notify(String notificationTitle, String notificationMsg, String notificationSubMsg, PendingIntent contentIntent) {
    	boolean doAlert = true;
        if (mContext.getSharedPreferences("main", 0).getBoolean("autoplay", false)) {
            return;
        }
                
        if (Push2TalkPresence.getInstance().isOnCall()) {
        	doAlert = false;
        }

        if (MusubiBaseActivity.getInstance().amResumed()) {
            // TODO: Filter per getInstance().getFeedUri(), but how do we get info here?
        	doAlert = false;
        }

        Notification notification = new Notification(
            R.drawable.icon, notificationTitle, System.currentTimeMillis());        

        notification.setLatestEventInfo(
            mContext, 
            notificationMsg, 
            notificationSubMsg, 
            contentIntent);
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE|Notification.FLAG_AUTO_CANCEL;

        if (doAlert) {
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            String uri = settings.getString("ringtone", null);

            //if (uri.equals("dungbeetle")) {
                notification.vibrate = VIBRATE;
            //}
            if(!uri.equals("none")) {
            	notification.sound = Uri.parse(uri);
            }
        }
        mNotificationManager.notify(NOTIFY_ID, notification);
    }

    public void cancelAll() {
        mNotificationManager.cancel(NOTIFY_ID);
    }
}