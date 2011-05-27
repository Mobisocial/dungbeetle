package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.apps.tag.record.UriRecord;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.appmanifest.ApplicationManifest;
import edu.stanford.mobisocial.appmanifest.platforms.PlatformReference;
import edu.stanford.mobisocial.bumblebee.util.Base64;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;

/**
 * Request/response protocol for pulling a remote context.
 * Handles SET_NDEF broadcasted intents, as sent by EasyNFC.
 *
 */
public class ActivitySnapshot extends BroadcastReceiver implements DbEntryHandler, ActivityCallout {
    private static final String TAG = "DbActivitySnapshot";
    public static final String TYPE = "app_snap";
    
    @Override
    public String getType() {
        return TYPE;
    }

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		// Not yet hndled.
	}

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
        // TODO Auto-generated method stub
        // mPackage, mAppArg
        return null;
    }

    @Override
    public void handleResult(int resultCode, Intent data) {
        // TODO Auto-generated method stub
        
    }
}
