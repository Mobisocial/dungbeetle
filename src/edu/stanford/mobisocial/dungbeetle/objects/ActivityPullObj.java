package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.apps.tag.record.UriRecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.bumblebee.util.Base64;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;

/**
 * Request/response protocol for pulling a remote context.
 * Handles SET_NDEF broadcasted intents, as sent by EasyNFC.
 *
 */
public class ActivityPullObj extends BroadcastReceiver implements DbEntryHandler {
    private static final String TAG = "activityPull";
    protected static final String ACTION_SET_NDEF = "mobisocial.intent.action.SET_NDEF";
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
		    }
		}
	}

	public static void activityForContact(Context context, Contact contact) {
	    toast(context, "Requesting activity...");
	    JSONObject activityPull = new JSONObject();
	    try {
	        activityPull.put(DbObject.TYPE, TYPE);
	        activityPull.put("request", "true");
	    } catch (JSONException e) {
	        Log.e(TAG, "Error building request", e);
	        return;
	    }
	    Helpers.sendMessage(context, contact, new DbObject(TYPE, activityPull));
	}

	private void startActivityForNdef(Context context, NdefMessage[] ndefMessages) {
        NdefRecord firstRecord = ndefMessages[0].getRecords()[0];
        Log.d(TAG, "DISCOVERED NDEF " + new String(firstRecord.getPayload()));

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
