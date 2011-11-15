package edu.stanford.mobisocial.dungbeetle.obj.handler;

import java.util.Iterator;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;

/**
 * Scans inbound objs for user information that can can be added as attributes.
 *
 */
public class ProfileScanningObjHandler implements IObjHandler {
    public static final String TAG = "musubi-profilescanner";
    public static final boolean DBG = true;

    @Override
    public void handleObj(Context context, DbEntryHandler handler, DbObj obj) {
        JSONObject json = obj.getJson();
        if (DBG) Log.d(TAG, "ProfileScanning obj " + json);
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String attr = iter.next();
            try {
                if (Contact.isWellKnownAttribute(attr)) {
                    if (DBG) Log.d(TAG, "Inserting attribute " + attr);
                    String val = json.getString(attr);
                    DbContactAttributes.update(context, obj.getSender().getLocalId(), attr, val);
                }
            } catch (JSONException e) {
                if (DBG) Log.w(TAG, "Could not pull attribute " + attr);
            }
        }
    }

}
