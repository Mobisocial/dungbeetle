package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.view.View;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.R;

public class StatusUpdate implements ObjectReceiver {
	public boolean handlesObject(JSONObject object) {
		return object.has("text");
	}
	public void render(View frame, JSONObject object) {
		String status = object.optString("text");
        TextView bodyText = (TextView)frame.findViewById(R.id.body_text);
        bodyText.setText(status);
	}
}