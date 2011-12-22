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

package edu.stanford.mobisocial.dungbeetle.feed.presence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PhoneStateObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

/**
 * Sends notice of received/sent phone calls.
 *
 */
public class PhonePresence extends FeedPresence {
    private boolean mSharePhoneState = false;
    private static String sPhoneNumber;

    @Override
    public String getName() {
        return "Phone";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mSharePhoneState) {
            if (getFeedsWithPresence().size() == 0) {
                context.getApplicationContext().unregisterReceiver(mReceiver);
                Toast.makeText(context, "No longer sharing phone state", Toast.LENGTH_SHORT).show();
                mSharePhoneState = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                IntentFilter iF = new IntentFilter();
                iF.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
                iF.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
                iF.setPriority(0);
                context.getApplicationContext().registerReceiver(mReceiver, iF);
                Toast.makeText(context, "Now sharing phone state", Toast.LENGTH_SHORT).show();
                mSharePhoneState = true;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
                sPhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                return;
            } else if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                sPhoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }
            if (mSharePhoneState &&
                    TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                String who = sPhoneNumber;
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                    sPhoneNumber = null;
                }
                for (Uri feedUri : getFeedsWithPresence()) {
                    Helpers.sendToFeed(context.getApplicationContext(), PhoneStateObj.from(state, who), feedUri);
                }
            }
        }
    };
}
