package edu.stanford.mobisocial.dungbeetle.social;

import edu.stanford.mobisocial.dungbeetle.HandleGroupSessionActivity;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ThreadRequest {
    public static final String PREFIX_JOIN = "//mobisocial.stanford.edu/musubi/thread";

	public static Uri getInvitationUri(Context c, Uri threadUri) {
        return new Uri.Builder()
        	.scheme("http")
        	.authority("mobisocial.stanford.edu")
        	.path("musubi/thread")
        	.appendQueryParameter("uri", threadUri.toString())
        	.build();
	}

	public static void acceptThreadRequest(Context c, Uri uri) {
	    Uri threadUri = Uri.parse(uri.getQueryParameter("uri"));
	    if (threadUri != null && threadUri.getScheme().equals(HomeActivity.GROUP_SESSION_SCHEME)) {
            Intent intent = new Intent().setClass(c, HandleGroupSessionActivity.class);
            intent.setData(threadUri);
            c.startActivity(intent);
	    } else {
	        Toast.makeText(c, "Error joining group.", Toast.LENGTH_SHORT).show();
	    }
	}
}
