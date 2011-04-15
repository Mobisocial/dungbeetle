package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.view.View;

public interface Renderable {
	public void renderToFeed(View frame, JSONObject content);
}
