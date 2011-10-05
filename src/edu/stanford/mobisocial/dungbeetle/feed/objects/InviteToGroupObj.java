package edu.stanford.mobisocial.dungbeetle.feed.objects;
import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;


public class InviteToGroupObj extends DbEntryHandler implements FeedRenderer, Activator {
	private static final String TAG = "InviteToGroupObj";
    public static final String TYPE = "invite_group";
    public static final String GROUP_NAME = "groupName";
    public static final String DYN_UPDATE_URI = "dynUpdateUri";
    public static final String PARTICIPANTS = "participants";
    public static final String SENDER = "sender";

    @Override
    public String getType() {
        return TYPE;
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    public static JSONObject json(String groupName, Uri dynUpdateUri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(GROUP_NAME, groupName);
            obj.put(DYN_UPDATE_URI, dynUpdateUri.toString());
        }
        catch(JSONException e){}
        return obj;
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

	public void handleDirectMessage(Context context, Contact from, JSONObject obj) {
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
	@Override
	public void render(Context context, ViewGroup frame, JSONObject content,
			byte[] raw, boolean allowInteractions) {

        TextView valueTV = new TextView(context);
        valueTV.setText("Join me in '" +content.optString(GROUP_NAME)+"'");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
	}
	@Override
	public void activate(Context context, long contactId, JSONObject content, byte[] raw) {
		// TODO Auto-generated method stub
		String groupName = content.optString(GROUP_NAME);
		Uri dynUpdateUri = Uri.parse(content.optString(DYN_UPDATE_URI));

		Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setData(dynUpdateUri);
		launch.putExtra("type", TYPE);
		launch.putExtra("creator", false);
		launch.putExtra(GROUP_NAME, groupName);
		launch.putExtra(DYN_UPDATE_URI, dynUpdateUri);
		context.startActivity(launch);
	}
}