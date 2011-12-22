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

package edu.stanford.mobisocial.dungbeetle.obj.handler;

import mobisocial.socialkit.musubi.DbObj;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.ui.FeedListActivity;
import edu.stanford.mobisocial.dungbeetle.ui.ViewContactActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

/**
 * Handles notifications associated with a received obj.
 * First, we check with the sender application to allow it to handle this data.
 * If the application does not indicate that we should not notify the user,
 * we check to see if the obj should be auto activated (for example, in tv mode).
 * Finally, we send a standard notification.
 *
 * An application prevents a notification event by setting the result data
 * to RESULT_CANCELLED (setResultCode(Activity.RESULT_CANCELLED)).
 *
 */
public class NotificationObjHandler extends ObjHandler {
    private static final int NO_NOTIFY = 0;
    private static final int NOTIFY = 1;
    private static final int AUTO_ACTIVATE = 2;

    private static final String ACTION_DATA_RECEIVED = "mobisocial.intent.action.DATA_RECEIVED";
    private static final String EXTRA_NOTIFICATION = "notification";
    private static final String EXTRA_OBJ_URI = "objUri";

    private final AutoActivateObjHandler mAutoActivate = new AutoActivateObjHandler();
    String TAG = "NotificationObjHandler";
    final DBHelper mHelper;

    public NotificationObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    BroadcastReceiver mAppHandler = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() != Activity.RESULT_OK) {
                return;
            }

            int notification = intent.getExtras().getInt(EXTRA_NOTIFICATION);
            Uri objUri = intent.getExtras().getParcelable(EXTRA_OBJ_URI);
            DbObj obj = App.instance().getMusubi().objForUri(objUri);
            if (notification == AUTO_ACTIVATE) {
             // Auto-activate without notification.
                DbEntryHandler handler = DbObjects.forType(obj.getType());
                mAutoActivate.handleObj(context, handler, obj);
            } else if (notification == NOTIFY) {
                Uri feedUri = obj.getContainingFeed().getUri();
                switch(Feed.typeOf(feedUri)) {
                    case FRIEND: {
                        Intent launch = new Intent().setClass(context, ViewContactActivity.class);
                        launch.putExtra("contact_id", obj.getSender().getLocalId());
                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                launch, PendingIntent.FLAG_CANCEL_CURRENT);
                        (new PresenceAwareNotify(context)).notify("New Musubi message",
                                "New Musubi message", "From " + obj.getSender().getName(),
                                contentIntent);
                        break;
                    }
                    case GROUP: {
                        String feedName = feedUri.getLastPathSegment();
                        Maybe<Group> group = mHelper.groupForFeedName(feedName);
                        Intent launch = new Intent(Intent.ACTION_VIEW);
                        launch.setClass(context, FeedListActivity.class);
                        if (Build.VERSION.SDK_INT < 11) {
                            launch.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        } else { 
                            launch.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        }

                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                launch, PendingIntent.FLAG_CANCEL_CURRENT);
                        try {
                            (new PresenceAwareNotify(context)).notify("New Musubi message",
                                    "New Musubi message", "In " + ((Group) group.get()).name,
                                    contentIntent);
                        } catch (NoValError e) {
                            Log.e(TAG, "No group while notifying for " + feedName);
                        }
                        break;
                    }
                    case RELATED: {
                        throw new RuntimeException("never should get a related feed from the network");
                    }
                }
            }
        }
    };

    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        int notification = NOTIFY;
        if (obj.getSender().getLocalId() == Contact.MY_ID) {
            notification = NO_NOTIFY;
        }

        if (handler == null || !(handler instanceof FeedRenderer)) {
            notification = NO_NOTIFY;
        }

        if (mAutoActivate.willActivate(context, obj)) {
            notification = AUTO_ACTIVATE;
        }

        if (!handler.doNotification(context, obj)) {
            notification = NO_NOTIFY;
        }

        // Let applications handle their own messages
        Intent objReceived = new Intent(ACTION_DATA_RECEIVED);
        objReceived.setPackage(obj.getAppId());
        objReceived.putExtra(EXTRA_NOTIFICATION, notification);
        objReceived.putExtra(EXTRA_OBJ_URI, obj.getUri());

        Bundle initialExtras = null;
        int initialCode = Activity.RESULT_OK;
        String initialData = null;
        Handler scheduler = null;
        String receiverPermission = null;
        context.sendOrderedBroadcast(objReceived, receiverPermission, mAppHandler, scheduler,
                initialCode, initialData, initialExtras);
    }
}
