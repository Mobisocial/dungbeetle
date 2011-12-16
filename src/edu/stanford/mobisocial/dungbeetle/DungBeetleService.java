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

import org.mobisocial.corral.ContentCorral;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * A persistent service for managing Musubi's long-lived tasks such
 * as network connectivity.
 */
public class DungBeetleService extends Service {
	private NotificationManager mNotificationManager;
	private MessagingManagerThread mMessagingManagerThread;
	private ContentCorral mContentCorral;
    private DBHelper mHelper;
    public static final String TAG = "DungBeetleService";


    @Override
    public void onCreate() {
        mHelper = DBHelper.getGlobal(this);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mMessagingManagerThread = new MessagingManagerThread(this);
        mMessagingManagerThread.start();

        // mPresenceThread = new PresenceThread(this);
        // mPresenceThread.start();

        // TODO: content corral should manage it's own ip ups and downs.
        if (ContentCorral.CONTENT_CORRAL_ENABLED) {
            mContentCorral = new ContentCorral(this);
            mContentCorral.start();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DungBeetleService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        mNotificationManager.cancel(R.string.active);
        Toast.makeText(this, R.string.stopping, Toast.LENGTH_SHORT).show();
        mHelper.close();
        mMessagingManagerThread.interrupt();
//        mPresenceThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new Binder(){
            DungBeetleService getService(){
                return DungBeetleService.this;
            }
        };


}
