package edu.stanford.mobisocial.dungbeetle.objects;
import org.json.JSONObject;
import android.view.View;

public interface FeedRenderer {
	public boolean willRender(JSONObject object);
	public void render(View frame, JSONObject content);
}
