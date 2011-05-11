package edu.stanford.mobisocial.dungbeetle.objects;
import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;


public class InviteToGroupObj implements IncomingMessageHandler {
	private static final String TAG = "InviteToGroupObj";
    public static final String TYPE = "invite_group";
    public static final String GROUP_NAME = "groupName";
    public static final String DYN_UPDATE_URI = "dynUpdateUri";
    public static final String PARTICIPANTS = "participants";
    public static final String SENDER = "sender";

    public static JSONObject json(String groupName, Uri dynUpdateUri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(GROUP_NAME, groupName);
            obj.put(DYN_UPDATE_URI, dynUpdateUri.toString());
        }
        catch(JSONException e){}
        return obj;
    }


	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		try {
			String groupName = obj.getString(GROUP_NAME);
			Uri dynUpdateUri = Uri.parse(obj.getString(DYN_UPDATE_URI));

			Intent launch = new Intent(Intent.ACTION_VIEW);
            launch.setData(dynUpdateUri);
			launch.putExtra("type", TYPE);
			launch.putExtra("creator", false);
			launch.putExtra(SENDER, from.id);
			launch.putExtra(GROUP_NAME, groupName);
			launch.putExtra(DYN_UPDATE_URI, dynUpdateUri);

            PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0,
                launch, 
                PendingIntent.FLAG_CANCEL_CURRENT);

            (new PresenceAwareNotify(context)).notify(
                "Invitation from " + from.name,
                "Invitation from " + from.name, 
                "Join the group '" + groupName + "'.", 
                contentIntent);

		} catch (JSONException e) {
			Log.e(TAG, "Error handling message: ", e);
		}
	}
}