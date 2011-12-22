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

package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    private static final String TAG = "msb-remoteReceiver";
    private static final boolean DBG = false;
    private static SpecialKeyEventHandler sSpecialKeyEventHandler;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            handleSpecialButton(context, (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
        }
    }

    public void handleSpecialButton(Context context, KeyEvent event) {
        if (DBG) Log.d(TAG, "Special key event received: " + event.getAction());
        if (sSpecialKeyEventHandler != null) {
            if (DBG) Log.d(TAG, "Trying registered handler");
            if (sSpecialKeyEventHandler.onSpecialKeyEvent(event)) {
                if (DBG) Log.d(TAG, "Key event consumed by handler");
                return;
            }
        }

        if (DBG) Log.d(TAG, "Default special key handler");
        if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            if (Push2TalkPresence.getInstance().isOnCall()) {
                Intent record = new Intent();
                record.setClass(context, VoiceQuickRecordActivity.class);
                record.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                record.putExtra("presence_mode", true);
                context.startActivity(record);
            }
        }
    }

    public interface SpecialKeyEventHandler {
        public boolean onSpecialKeyEvent(KeyEvent event);
    }

    public static void setSpecialKeyEventHandler(SpecialKeyEventHandler h) {
        sSpecialKeyEventHandler = h;
    }

    public static void clearSpecialKeyEventHandler() {
        sSpecialKeyEventHandler = null;
    }
}