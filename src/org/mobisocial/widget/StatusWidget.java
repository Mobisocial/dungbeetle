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

package org.mobisocial.widget;

import mobisocial.socialkit.musubi.DbObj;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

public class StatusWidget extends AppWidgetProvider {
    static final String TAG = "musubi-widget";
    final static Uri feedUri = Feed.uriForName(Feed.FEED_NAME_GLOBAL);

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }
    
    public static class UpdateService extends Service {
        ContentObserver mContentObserver;

        @Override
        public void onStart(Intent intent, int startId) {
            updateWidget();

            mContentObserver = new ContentObserver(new Handler(getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    updateWidget();
                }
            };
            getContentResolver().registerContentObserver(feedUri, false, mContentObserver);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void updateWidget() {
            RemoteViews updateViews = buildUpdate(UpdateService.this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(UpdateService.this, StatusWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        public static RemoteViews buildUpdate(Context context) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_message);
            String status;

            String[] projection = new String[]
                    { DbObj.COL_ID, DbObj.COL_JSON, DbObj.COL_CONTACT_ID, DbObj.COL_FEED_NAME };
            String selection = "type = ?";
            String[] selectionArgs = new String[] { StatusObj.TYPE };
            String sortOrder = DbObj.COL_ID + " desc limit 1";
            Cursor c = context.getContentResolver().query(feedUri, projection,
                    selection, selectionArgs, sortOrder);

            PendingIntent pendingIntent;
            if (c != null && c.moveToFirst()) {
                DbObj obj = App.instance().getMusubi().objForCursor(c);
                if (obj == null || obj.getSender() == null) {
                    return null;
                }
                status = obj.getSender().getName() + ": " +
                        obj.getJson().optString(StatusObj.TEXT);

                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(obj.getContainingFeed().getUri(), Feed.MIME_TYPE);
                pendingIntent = PendingIntent.getActivity(context, 0, viewIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
            } else {
                status = "No messages found.";
                pendingIntent = null;
            }
            views.setTextViewText(R.id.message, status);
            views.setOnClickPendingIntent(R.id.message, pendingIntent);
            return views;
        }
    }
}