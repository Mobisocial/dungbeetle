
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

import android.graphics.Bitmap;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.os.Environment;
import android.content.Intent;


import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import android.net.Uri;
import android.util.Log;

/**
 * Sends a picture object using the standard Android "SEND" intent.
 *
 */
public class ExportPhotoAction extends ObjAction {
    public void onAct(Context context, Uri feedUri, DbEntryHandler objType, long hash, JSONObject objData, byte[] raw) {
        String b64Bytes = objData.optString(PictureObj.DATA);
        if (raw == null) {
        	raw = FastBase64.decode(b64Bytes);
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
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);  
            intent.setType("image/png");
            Log.w("ResharePhotoAction", Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_share.png");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file)); 
            context.startActivity(Intent.createChooser(intent, "Export image to"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public String getLabel(Context context) {
        return "Export";
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        /*if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }*/
        return (objType instanceof PictureObj);
    }
}
