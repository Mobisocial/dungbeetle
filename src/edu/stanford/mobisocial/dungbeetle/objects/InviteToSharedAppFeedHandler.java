package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.widget.Toast;

import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.InviteObj;

public class InviteToSharedAppFeedHandler extends MessageHandler {
	private static final String TAG = "InviteToSharedAppFeedHandler";

	public InviteToSharedAppFeedHandler(Context context) {
		super(context);
		mContext = context;
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("invite_app_feed");
	}

	public void handleReceived(Contact from, JSONObject obj) {
		try {
			String packageName = obj.getString(InviteObj.PACKAGE_NAME);
			String feedName = obj.getString("sharedFeedName");
			JSONArray ids = obj.getJSONArray(InviteObj.PARTICIPANTS);
			Intent launch = new Intent();
			launch.setAction(Intent.ACTION_MAIN);
			launch.addCategory(Intent.CATEGORY_LAUNCHER);
			launch.putExtra("type", "invite_app_feed");
			launch.putExtra("creator", false);
			launch.putExtra("sender", from.id);
			launch.putExtra("sharedFeedName", feedName);
			launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			long[] idArray = new long[ids.length()];
			for (int i = 0; i < ids.length(); i++) {
				idArray[i] = ids.getLong(i);
			}
			launch.putExtra("participants", idArray);
			launch.setPackage(packageName);
			final PackageManager mgr = mContext.getPackageManager();
			List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
			if (resolved.size() == 0) {
				Toast.makeText(mContext,
						"Could not find application to handle invite.",
						Toast.LENGTH_SHORT).show();
				return;
			}
			ActivityInfo info = resolved.get(0).activityInfo;
			launch.setComponent(new ComponentName(info.packageName, info.name));
			PendingIntent contentIntent = PendingIntent.getActivity(mContext,
					0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

			getPresenceAwareNotify().notify("New Invitation from " + from.name,
					"Invitation received from " + from.name,
					"Click to launch application: " + packageName,
					contentIntent);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}