package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

class IMHandler extends MessageHandler {
	public IMHandler(Context c) {
		super(c);
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("instant_message");
	}

	public void handleReceived(Contact from, JSONObject obj) {
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.setComponent(new ComponentName(mContext.getPackageName(),
				DungBeetleActivity.class.getName()));
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				launch, PendingIntent.FLAG_CANCEL_CURRENT);

		String msg = obj.optString("text");

		getPresenceAwareNotify().notify("IM from " + from.name,
				"IM from " + from.name, "\"" + msg + "\"", contentIntent);
	}
}
