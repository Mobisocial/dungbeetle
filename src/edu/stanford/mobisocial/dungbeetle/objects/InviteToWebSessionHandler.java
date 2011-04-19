package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.InviteObj;

class InviteToWebSessionHandler extends MessageHandler {
	public InviteToWebSessionHandler(Context context) {
		super(context);
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("invite_web_session");
	}

	public void handleReceived(Contact from, JSONObject obj) {

		String arg = obj.optString(InviteObj.ARG);
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
		launch.putExtra("creator", false);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String webUrl = obj.optString(InviteObj.WEB_URL);
		launch.setData(Uri.parse(webUrl));

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				launch, PendingIntent.FLAG_CANCEL_CURRENT);

		getPresenceAwareNotify().notify("New Invitation",
				"Invitation received", "Click to launch application.",
				contentIntent);
	}
}