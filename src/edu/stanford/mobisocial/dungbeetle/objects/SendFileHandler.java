package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class SendFileHandler extends MessageHandler {
	public SendFileHandler(Context context) {
		super(context);
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("send_file");
	}

	public void handleReceived(Contact from, JSONObject obj) {
		String mimeType = obj.optString("mimeType");
		String uri = obj.optString("uri");
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.setType(mimeType);
		i.setData(Uri.parse(uri));
		i.putExtra(Intent.EXTRA_TEXT, uri);

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, i,
				PendingIntent.FLAG_CANCEL_CURRENT);

		getPresenceAwareNotify().notify("New Shared File...",
				"New Shared File", mimeType + "  " + uri, contentIntent);
	}
}