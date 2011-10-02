
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

public class PassItOnAction extends ObjAction {
    private static final String TAG = "passItOn";
    private static JSONObject mJson;
    private static DbEntryHandler mType;
    private Context mContext;

    public void onAct(Context context, Uri feedUri, DbEntryHandler objType, long hash, JSONObject objData, byte[] raw) {
        mContext = context;
    	objData = objType.mergeRaw(objData, raw);
        holdObj(context, objType, objData);
        ((InstrumentedActivity)context).doActivityForResult(mTargetSelected);
    }

    @Override
    public String getLabel(Context context) {
        return "Pass it On";
    }

    //Helpers.sendToFeed(context, new DbObject(mType, mJson), feedUri);

    public static void holdObj(Context context, DbEntryHandler type, JSONObject json) {
        mType = type;
        mJson = json;

        // TODO: Expand support by allowing to "paste" to another app
        // in the picker.
        /*
        if (json.has(StatusObj.TEXT)) {
            ClipboardManager m = (ClipboardManager)context.getSystemService(
                    Context.CLIPBOARD_SERVICE);
            m.setText(json.optString(StatusObj.TEXT));
        }*/
    }

    private ActivityCallout mTargetSelected = new ActivityCallout() {
        @Override
        public void handleResult(int resultCode, Intent data) {
            Parcelable[] feedParc = (Parcelable[])data.getParcelableArrayExtra(PickContactsActivity.EXTRA_FEEDS);
            Uri[] uris = new Uri[feedParc.length];
            int i = 0;
            for (Parcelable p : feedParc) {
                uris[i++] = (Uri)p;
            }
            Helpers.sendToFeeds(mContext, mType.getType(), mJson, uris);
        }
        
        @Override
        public Intent getStartIntent() {
            // TODO:
            //return new Intent(Intent.ACTION_PICK, Contact.MIME_TYPE);
            Intent picker = new Intent(mContext, PickContactsActivity.class);
            picker.setAction(Intent.ACTION_PICK);
            return picker;
        }
    };

    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        mContext = context;
        return MusubiBaseActivity.isDeveloperModeEnabled(context);
    }
}
