package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import android.net.Uri;

public class PresenceAwareNotify {
	private int notifyCounter = 0;
	private NotificationManager mNotificationManager;
	private final long[] VIBRATE = new long[] {0, 250, 80, 100, 80, 80, 80, 250};
	Context mContext;

    public PresenceAwareNotify(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager)
            context.getSystemService(context.NOTIFICATION_SERVICE);
    }
        
    public void notify(String notificationTitle, String notificationMsg, String notificationSubMsg, PendingIntent contentIntent) {
        Notification notification = new Notification(
            R.drawable.icon, notificationTitle, System.currentTimeMillis());


        notification.vibrate = VIBRATE;


        // Disable vibrate if busy
        Cursor c = mContext.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
            null, 
            Object.TYPE + "=?", 
            new String[]{ "presence"}, 
            Object.TIMESTAMP + " DESC");
        c.moveToFirst();
        if(!c.isAfterLast()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
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
        mNotificationManager.notify(nextNotifyId(), notification);
    }
        
    private int nextNotifyId(){
        notifyCounter += 1;
        return notifyCounter;
    }
}