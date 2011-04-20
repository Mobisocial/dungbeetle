package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.R;

public class IMObj implements IncomingMessageHandler, FeedRenderer {
    public static final String TYPE = "instant_message";
    public static final String TEXT = "text";

    public static JSONObject json(String msg){
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", msg);
        }catch(JSONException e){}
        return obj;
    }

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		Intent launch = new Intent();
		launch.setAction(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.setComponent(new ComponentName(context.getPackageName(),
                                              DungBeetleActivity.class.getName()));
		PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0,
            launch, PendingIntent.FLAG_CANCEL_CURRENT);
		String msg = obj.optString(TEXT);
		(new PresenceAwareNotify(context)).notify(
            "IM from " + from.name,
            "IM from " + from.name, "\"" + msg + "\"", contentIntent);
	}


	public boolean willRender(JSONObject object) {
		return object.optString("type").equals(TYPE);
	}

	public void render(Context context, ViewGroup frame, JSONObject content){
        TextView valueTV = new TextView(context);
        valueTV.setText("IM:" + content.optString(TEXT));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
