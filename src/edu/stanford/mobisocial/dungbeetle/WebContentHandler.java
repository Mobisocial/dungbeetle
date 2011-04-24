package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * A helper Activity for accepting web-friendly
 * DungBeetle URIs.
 *
 */
public class WebContentHandler extends Activity {
	public static final Uri DUNGBEETLE_WEB_URI = Uri.parse("http://mobisocial.stanford.edu/dungbeetle");

	/**
	 * Wraps a DungBeetle uri so it can be used wherever
	 * web URLs are allowed.
	 */
	public static Uri getWebFriendlyUri(Uri dungbeetleUri) {
		return DUNGBEETLE_WEB_URI.buildUpon()
			.appendQueryParameter("content", dungbeetleUri.toString())
			.build();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Uri content = getIntent().getData();
		Uri action = Uri.parse(content.getQueryParameter("content"));
		Intent launch = new Intent(Intent.ACTION_VIEW);
		launch.setData(action);
		startActivity(launch);
		finish();
	}
}
