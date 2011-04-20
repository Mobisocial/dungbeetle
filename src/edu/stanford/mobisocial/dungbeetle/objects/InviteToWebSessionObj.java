package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.R;

public class InviteToWebSessionObj implements IncomingMessageHandler, FeedRenderer {

    public static final String TYPE = "invite_web_session";
    public static final String WEB_URL = "webUrl";
    public static final String ARG = "arg";

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
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

	public boolean willRender(JSONObject object) { return false; }
	public void render(Context context, ViewGroup frame, JSONObject content){}


}