package edu.stanford.mobisocial.dungbeetle;

import org.json.JSONObject;

/**
 * Working out the details of received messages, database objects,
 * and feed-renderable content.   Objects come in from the wire,
 * are recognized by a Builder, become instantiated FeedObjects.
 * FeedObjects can insert themselves into a database or take 
 * some other action (notification, etc.)
 * 
 * We somehow need to populate a list of Renderables to query from the database.
 * We can use a list of TYPEs and query against them, or create a 'Renderable'
 * column or something.
 * 
 * If we have a list of types, then renderable should be on the Builder
 * so we can create the list (since we'll have a set of instantiated builders)
 *
 */
public interface FeedObject {
	public void toValues();
	
	public interface Builder {
		public boolean handlesJson(JSONObject obj);
		public FeedObject fromJson(JSONObject obj);
	}

	public interface Renderable {
		
	}
}
