package edu.stanford.mobisocial.dungbeetle.model;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class PresenceAwareNotify {
	public static final int NOTIFY_ID = 9847184;
	private NotificationManager mNotificationManager;
	private final long[] VIBRATE = new long[] {0, 250, 80, 100, 80, 80, 80, 250};
	Context mContext;

    public PresenceAwareNotify(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
        
    public void notify(String notificationTitle, String notificationMsg, String notificationSubMsg, PendingIntent contentIntent) {

        if (Push2TalkPresence.getInstance().isOnCall()) {
            return;
        }

        if (MusubiBaseActivity.getInstance().amResumed()) {
            // TODO: Filter per getInstance().getFeedUri(), but how do we get info here?
            return;
        }

        Notification notification = new Notification(
            R.drawable.icon, notificationTitle, System.currentTimeMillis());

        notification.vibrate = VIBRATE;
        // Disable vibrate if busy
        Cursor c = mContext.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
            null, 
            DbObject.TYPE + "=?", 
            new String[]{ "presence"}, 
            DbObject.TIMESTAMP + " DESC");
        c.moveToFirst();
        if(!c.isAfterLast()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                int myPresence = Integer.parseInt(obj.optString("presence"));
                if(myPresence == Presence.BUSY) {
                    notification.vibrate = null;
                }
            }catch(JSONException e){}
        }


        notification.setLatestEventInfo(
            mContext, 
            notificationMsg, 
            notificationSubMsg, 
            contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(NOTIFY_ID, notification);
    }

    public void cancelAll() {
        mNotificationManager.cancel(NOTIFY_ID);
    }
}