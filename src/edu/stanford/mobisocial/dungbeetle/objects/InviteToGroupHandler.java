package edu.stanford.mobisocial.dungbeetle.objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.InviteObj;

public class InviteToGroupHandler extends MessageHandler {
	private static final String TAG = "InviteToGroupHandler";

	public InviteToGroupHandler(Context context) {
		super(context);
		mContext = context;
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("invite_group");
	}

	public void handleReceived(Contact from, JSONObject obj) {
		try {
			String groupName = obj.getString("groupName");
			String feedName = obj.getString("sharedFeedName");
			JSONArray ids = obj.getJSONArray(InviteObj.PARTICIPANTS);
			Intent launch = new Intent();
			launch.setAction(Intent.ACTION_MAIN);
			launch.addCategory(Intent.CATEGORY_LAUNCHER);
			launch.putExtra("type", "invite_group");
			launch.putExtra("creator", false);
			launch.putExtra("sender", from.id);
			launch.putExtra("sharedFeedName", feedName);
			launch.putExtra("groupName", groupName);
			long[] idArray = new long[ids.length()];
			for (int i = 0; i < ids.length(); i++) {
				idArray[i] = ids.getLong(i);
			}
			launch.putExtra("participants", idArray);
            launch.setAction("edu.stanford.mobisocial.dungbeetle.HANDLE_GROUP_INVITE");
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                                                                    launch, 
                                                                    PendingIntent.FLAG_CANCEL_CURRENT);
            getPresenceAwareNotify().notify("Invitation from " + from.name,
                                            "Invitation from " + from.name, 
                                            "Join '" + groupName + "' with " + 
                                            (idArray.length - 1) + " others.", 
                                            contentIntent);

		} catch (JSONException e) {
			Log.e(TAG, "Error handling message: ", e);
		}
	}
}