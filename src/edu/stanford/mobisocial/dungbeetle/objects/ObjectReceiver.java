package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.view.View;

public interface ObjectReceiver {
	public boolean handlesObject(JSONObject object);
	public void render(View frame, JSONObject content);
}
