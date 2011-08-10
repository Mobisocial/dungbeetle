package edu.stanford.mobisocial.dungbeetle.feed.objects;

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
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * Request/response protocol for pulling a remote context.
 * Handles SET_NDEF broadcasted intents, as sent by EasyNFC.
 *
 */
public class ActivityPullObj extends BroadcastReceiver implements DbEntryHandler {
    private static final String TAG = "activityPull";
    protected static final String ACTION_SET_NDEF = "mobisocial.intent.action.SET_NDEF";
    public static final String EXTRA_APPLICATION_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";
    public static final String TYPE = "ndef_pull";
    private static NdefMessage mNdef;
    
    @Override
    public String getType() {
        return TYPE;
    }

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		if (obj.has("request")) {
		    JSONObject response = new JSONObject();
		    try {
		        response.put(DbObject.TYPE, TYPE);
		        response.put("response", "true");
		        if (mNdef != null) {
		            response.put("ndef", Base64.encodeToString(mNdef.toByteArray(), false));
		        }
		    } catch (JSONException e) {
		        Log.e(TAG, "Error building response", e);
	            return;    
		    }
		    Helpers.sendMessage(context, from, new DbObject(TYPE, response));
		} else if (obj.has("response")) {
		    if (obj.has("ndef")) {
		        try {
		            byte[] ndefBytes = Base64.decode(obj.optString("ndef"));
		            NdefMessage ndef = new NdefMessage(ndefBytes);
		            startActivityForNdef(context, new NdefMessage[] { ndef });
		        } catch (Exception e) {
		            Log.e(TAG, "error receiving ndef", e);
		            toast(context, "Could not decode message.");
		        }
		    } else {
		        toast(context, "No activity found.");
		    }
		}
	}

	public static void activityForContact(Context context, Contact contact) {
	    toast(context, "Requesting activity...");
	    JSONObject activityPull = new JSONObject();
	    try {
	        activityPull.put("request", "true");
	    } catch (JSONException e) {
	        Log.e(TAG, "Error building request", e);
	        return;
	    }
	    Helpers.sendMessage(context, contact, new DbObject(TYPE, activityPull));
	}

	private void startActivityForNdef(Context context, NdefMessage[] ndefMessages) {
	    NdefMessage ndef = ndefMessages[0];
        NdefRecord firstRecord = ndef.getRecords()[0];

        if (UriRecord.isUri(firstRecord)) {
            UriRecord uriRecord = UriRecord.parse(firstRecord);
            Intent intent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED, uriRecord.getUri());
            intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, ndefMessages);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (null == context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
                toast(context, "Could not handle ndef.");
                return;
            }
            context.startActivity(intent);
        } else if (firstRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
            Intent launch = null;
            String webpage = null;
            String androidReference = null;

            byte[] manifestBytes = ndef.getRecords()[0].getPayload();
            ApplicationManifest manifest = new ApplicationManifest(manifestBytes);
            List<PlatformReference> platforms = manifest
                    .getPlatformReferences();
            for (PlatformReference platform : platforms) {
                int platformId = platform.getPlatformIdentifier();
                switch (platformId) {
                case ApplicationManifest.PLATFORM_WEB_GET:
                    webpage = new String(platform.getAppReference());
                    break;
                case ApplicationManifest.PLATFORM_ANDROID_PACKAGE:
                    androidReference = new String(platform.getAppReference());
                    break;
                }
            }

            boolean foundMatch = false;
            if (androidReference != null) {
                int col = androidReference.indexOf(":");
                String pkg = androidReference.substring(0, col);
                String arg = androidReference.substring(col+1);
                
                launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setPackage(pkg);
                launch.putExtra(EXTRA_APPLICATION_ARGUMENT, arg);
                
                // TODO: support applications that aren't yet installed.
                List<ResolveInfo> resolved = context.getPackageManager().queryIntentActivities(launch, 0);
                if (resolved != null && resolved.size() > 0) {
                    ActivityInfo info = resolved.get(0).activityInfo;
                    launch.setComponent(new ComponentName(info.packageName, info.name));
                    foundMatch = true;
                }
            }

            if (!foundMatch && webpage != null) {
                launch = new Intent(Intent.ACTION_VIEW);
                launch.setData(Uri.parse(webpage));
                foundMatch = true;
            }

            if (foundMatch) {
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
            } else {
                toast(context, "Failed to launch activity.");
            }
        } else {
            toast(context, "Ndef launching needs work.");
        }
    }
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (!intent.getAction().equals(ACTION_SET_NDEF)) {
	        return;
	    }
	    if (intent.hasExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)) {
	        mNdef = (NdefMessage)intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
	    } else {
	        mNdef = null;
	    }
	}

	private static void toast(final Context context, final String text) {
	    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
	}
}
