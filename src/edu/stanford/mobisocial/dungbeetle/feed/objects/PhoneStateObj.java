package edu.stanford.mobisocial.dungbeetle.feed.objects;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import org.json.JSONException;
import org.json.JSONObject;

public class PhoneStateObj implements DbEntryHandler, FeedRenderer {

    public static final String EXTRA_STATE_UNKNOWN = "UNKNOWN";
    public static final String TYPE = "phone";
    public static final String ACTION = "action";
    public static final String NUMBER = "num";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String action, String number) {
        return new DbObject(TYPE, json(action, number));
    }

    public static JSONObject json(String action, String number){
        JSONObject obj = new JSONObject();
        try{
            obj.put(ACTION, action);
            obj.put(NUMBER, number);
        }catch(JSONException e){}
        return obj;
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		return objData;
	}

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		return null;
	}

    public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText(asText(content));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    private String asText(JSONObject obj) {
        StringBuilder status = new StringBuilder();
        String a = obj.optString(ACTION);
        String b = obj.optString(NUMBER);
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(a)) {
            status.append("Calling ");
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(a)) {
            status.append("Ending phone call with ");
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(a)) {
            status.append("Inbound call from ");
        }
        status.append(b).append(".");
        return status.toString();
    }
}
