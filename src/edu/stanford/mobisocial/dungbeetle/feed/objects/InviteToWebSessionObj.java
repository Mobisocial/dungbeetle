package edu.stanford.mobisocial.dungbeetle.feed.objects;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

public class InviteToWebSessionObj implements DbEntryHandler {

    public static final String TYPE = "invite_web_session";
    public static final String WEB_URL = "webUrl";
    public static final String ARG = "arg";

    @Override
    public String getType() {
        return TYPE;
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		String arg = obj.optString(ARG);
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
		launch.putExtra("creator", false);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String webUrl = obj.optString(WEB_URL);
		launch.setData(Uri.parse(webUrl));
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				launch, PendingIntent.FLAG_CANCEL_CURRENT);
		(new PresenceAwareNotify(context)).notify("New Invitation",
				"Invitation received", "Click to launch application.",
				contentIntent);
	}
}