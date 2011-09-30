package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.util.Collection;
import java.util.Iterator;

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
import android.util.Pair;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;

public class InviteToSharedAppFeedObj extends DbEntryHandler {
	private static final String TAG = "InviteToSharedAppFeedHandler";

    public static final String TYPE = "invite_app_feed";
    public static final String ARG = "arg";
    public static final String PACKAGE_NAME = "packageName";
    public static final String PARTICIPANTS = "participants";
    public static final String FEED_NAME = "feedName";

    @Override
    public String getType() {
        return TYPE;
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    public static JSONObject json(Collection<Contact> contacts, 
                                  String feedName,
                                  String packageName){
        JSONObject obj = new JSONObject();
        try{
            obj.put("packageName", packageName);
            obj.put("sharedFeedName", feedName);
            JSONArray participants = new JSONArray();
            Iterator<Contact> it = contacts.iterator();
            while(it.hasNext()){
                String localId = "@l" + it.next().id;
                participants.put(participants.length(), localId);
            }
            // Need to add ourself to participants
            participants.put(participants.length(), "@l" + Contact.MY_ID);
            obj.put("participants", participants);
        }catch(JSONException e){}
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
		try {
			String packageName = obj.getString(PACKAGE_NAME);
			String feedName = obj.getString("sharedFeedName");
			JSONArray ids = obj.getJSONArray(PARTICIPANTS);
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
			final PackageManager mgr = context.getPackageManager();
			List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
			if (resolved.size() == 0) {
				Toast.makeText(
                    context,
                    "Could not find application to handle invite.",
                    Toast.LENGTH_SHORT).show();
				return;
			}
			ActivityInfo info = resolved.get(0).activityInfo;
			launch.setComponent(new ComponentName(info.packageName, info.name));
			PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

			(new PresenceAwareNotify(context)).notify(
                "New Invitation from " + from.name,
                "Invitation received from " + from.name,
                "Click to launch application: " + packageName,
                contentIntent);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}