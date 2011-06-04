package edu.stanford.mobisocial.dungbeetle.objects;


import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.objects.iface.IntentAbsorber;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;

/**
 * Request/response protocol for pulling a remote context.
 * Handles SET_NDEF broadcasted intents, as sent by EasyNFC.
 *
 */
public class ActivitySnapshot extends BroadcastReceiver implements DbEntryHandler, ActivityCallout {
    @SuppressWarnings("unused")
    private static final String TAG = "DbActivitySnapshot";
    public static final String TYPE = "app_snapshot";
    
    @Override
    public String getType() {
        return TYPE;
    }

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		// Not yet handled.
	}

	@SuppressWarnings("unused")
	private static void toast(final Context context, final String text) {
	    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals("mobisocial.db.APP_SNAPSHOT")) {
	        // PUT JSON IN FEED
	    }
	}

	
	
	// ActivityCallout

    @Override
    public Intent getStartIntent() {
        // mPackage, mAppArg
        return null;
    }

    @Override
    public void handleResult(int resultCode, Intent data) {
        // TODO Auto-generated method stub
        
    }
}
