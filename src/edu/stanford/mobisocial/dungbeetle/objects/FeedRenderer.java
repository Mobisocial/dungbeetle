package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import android.view.ViewGroup;
import org.json.JSONObject;


public interface FeedRenderer {
	public boolean willRender(JSONObject object);
	public void render(Context context, ViewGroup frame, JSONObject content);
}
