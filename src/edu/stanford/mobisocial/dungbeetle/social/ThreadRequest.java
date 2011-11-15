package edu.stanford.mobisocial.dungbeetle.social;

import edu.stanford.mobisocial.dungbeetle.HandleGroupSessionActivity;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ThreadRequest {
    public static final String PREFIX_JOIN = "//group/invite";

	public static void acceptThreadRequest(Context c, Uri uri) {
	    Uri threadUri = Uri.parse(uri.getQueryParameter("uri"));
	    if (threadUri != null && threadUri.getScheme().equals(HomeActivity.SCHEME)) {
            Intent intent = new Intent().setClass(c, HandleGroupSessionActivity.class);
            intent.setData(threadUri);
            c.startActivity(intent);
	    } else {
	        Toast.makeText(c, "Error joining group.", Toast.LENGTH_SHORT).show();
	    }
	}
}
