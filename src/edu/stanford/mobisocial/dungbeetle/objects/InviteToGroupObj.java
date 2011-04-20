package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.R;


public class InviteToGroupObj implements IncomingMessageHandler, FeedRenderer {
	private static final String TAG = "InviteToGroupObj";

    public static final String TYPE = "invite_group";
    public static final String SHARED_FEED_NAME = "sharedFeedName";
    public static final String GROUP_NAME = "groupName";
    public static final String PARTICIPANTS = "participants";

    public static JSONObject json(
        long[] participants, String groupName, String feedName){
        JSONObject obj = new JSONObject();
        try{
            obj.put(GROUP_NAME, groupName);
            obj.put(SHARED_FEED_NAME, feedName);
            JSONArray parts = new JSONArray();
            for(int i = 0; i < participants.length; i++){
                String localId = "@l" + participants[i];
                parts.put(i, localId);
            }
            // Need to add ourself to participants
            parts.put(parts.length(), "@l" + Contact.MY_ID);
            obj.put(PARTICIPANTS, parts);
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
			String feedName = obj.getString(SHARED_FEED_NAME);
			JSONArray ids = obj.getJSONArray(PARTICIPANTS);
			Intent launch = new Intent();
			launch.setAction(Intent.ACTION_MAIN);
			launch.addCategory(Intent.CATEGORY_LAUNCHER);
			launch.putExtra("type", TYPE);
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
            PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0,
                launch, 
                PendingIntent.FLAG_CANCEL_CURRENT);
            (new PresenceAwareNotify(context)).notify(
                "Invitation from " + from.name,
                "Invitation from " + from.name, 
                "Join '" + groupName + "' with " + 
                (idArray.length - 1) + " other(s).", 
                contentIntent);

		} catch (JSONException e) {
			Log.e(TAG, "Error handling message: ", e);
		}
	}


	public boolean willRender(JSONObject object) { return false; }
	public void render(Context context, ViewGroup frame, JSONObject content){}

}