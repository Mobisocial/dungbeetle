package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import org.json.JSONObject;

public interface Activator {
	public void activate(Context context, JSONObject content);
	public boolean willActivate(JSONObject content);
}
