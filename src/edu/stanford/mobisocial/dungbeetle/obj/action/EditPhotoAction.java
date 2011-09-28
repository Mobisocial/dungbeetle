
package edu.stanford.mobisocial.dungbeetle.obj.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

/**
 * Sends a picture object using the standard Android "SEND" intent.
 *
 */
public class EditPhotoAction extends ObjAction {
    public void onAct(Context context, Uri feedUri,
            DbEntryHandler objType, JSONObject objData, byte[] raw) {
        ((InstrumentedActivity)context).doActivityForResult(
                new EditCallout(context, feedUri, objData, raw));
    }

    @Override
    public String getLabel() {
        return "Edit";
    }

    @Override
    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        /*if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }*/
        return (objType instanceof PictureObj);
    }

    private class EditCallout implements ActivityCallout {
        final JSONObject mJson;
        final byte[] mRaw;
        final Context mContext;
        final Uri mFeedUri;

        public EditCallout(Context context, Uri feedUri, JSONObject json, byte[] raw) {
            mJson = json;
            mRaw = raw;
            mContext = context;
            mFeedUri = feedUri;
        }
        @Override
        public Intent getStartIntent() {
            byte[] raw;
            if (mRaw == null) {
                String b64Bytes = mJson.optString(PictureObj.DATA);
                raw = Base64.decode(b64Bytes);
            } else {
                raw = mRaw;
            }
            OutputStream outStream = null;
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_share.png");
            try {
                outStream = new FileOutputStream(file);
                
                BitmapManager mgr = new BitmapManager(1);
                Bitmap bitmap = mgr.getBitmap(raw.hashCode(), raw);
                
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();

                bitmap.recycle();
                bitmap = null;
                System.gc();
                Intent intent = new Intent(Intent.ACTION_EDIT);  
                intent.setDataAndType(Uri.fromFile(file), "image/png");
                return Intent.createChooser(intent, "Edit with");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void handleResult(int resultCode, final Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            // TODO: mimeType; local_uri = data.toString();
                            DbObject outboundObj = PictureObj.from(mContext, data.getData());
                            Helpers.sendToFeed(mContext, outboundObj, mFeedUri);
                        } catch (IOException e) {
                            Toast.makeText(mContext, "Error reading photo data.", Toast.LENGTH_SHORT).show();
                            Log.e(HomeActivity.TAG, "Error reading photo data.", e);
                        }
                    }
                }.start();
            }
        }
    }
}
