
package edu.stanford.mobisocial.dungbeetle.obj.action;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbRelation;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

/**
 * Edits a picture object using the standard Android "EDIT" intent.
 *
 */
public class EditPhotoAction extends ObjAction {
    private static final String TAG = "EditPhotoAction";

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {

        ((InstrumentedActivity)context).doActivityForResult(
                new EditCallout((Activity)context, obj));
    }

    @Override
    public String getLabel(Context context) {
        return "Edit";
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        /*if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }*/
        return (objType instanceof PictureObj);
    }

    public static class EditCallout implements ActivityCallout {
        final JSONObject mJson;
        final byte[] mRaw;
        final Activity mContext;
        final Uri mFeedUri;
        final Uri mHdUri;
        final long mHash;

        public EditCallout(Activity context, DbObj obj) {
            mHash = obj.getHash();
            mJson = obj.getJson();
            mRaw = obj.getRaw();
            mContext = context;
            mFeedUri = obj.getContainingFeed().getUri();
            Uri hd = null;
            if (ContentCorral.fileAvailableLocally(context, obj)) {
                try {
                    hd = ContentCorral.fetchContent(context, obj);
                } catch (IOException e) {}
            }
            mHdUri = hd;
        }
        @Override
        public Intent getStartIntent() {
            Uri contentUri;

            File file;
            if (mHdUri != null) {
                // Don't edit in-place to avoid edited images showing up in
                // places like the camera reel.
                String extension = ".tmp";
                String fname = mHdUri.getLastPathSegment();
                if (fname.contains(".")) {
                    extension = fname.substring(fname.indexOf('.'));
                }
                try {
                    file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/temp_share" + extension);
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream is = mContext.getContentResolver().openInputStream(mHdUri);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                    Matrix matrix = new Matrix();
                    float rotation = PhotoTaker.rotationForImage(mContext, mHdUri);
                    if (rotation != 0f) {
                        matrix.preRotate(rotation);
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                    }
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    Toast.makeText(mContext, "Could not edit photo.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error editing photo", e);
                    return null;
                }
            } else {
                byte[] raw;
                if (mRaw == null) {
                    String b64Bytes = mJson.optString(PictureObj.DATA);
                    raw = FastBase64.decode(b64Bytes);
                } else {
                    raw = mRaw;
                }
                OutputStream outStream = null;
                try {
                    file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/temp_share.png");
                    outStream = new FileOutputStream(file);
                    BitmapManager mgr = new BitmapManager(1);
                    Bitmap bitmap = mgr.getBitmap(raw.hashCode(), raw);
                    if(bitmap == null)
                    	return null;
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    bitmap.recycle();
                    bitmap = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            contentUri = Uri.fromFile(file);

            Intent intent = new Intent(Intent.ACTION_EDIT);  
            intent.setDataAndType(contentUri, "image/png");
            intent.putExtra(Musubi.EXTRA_FEED_URI, mFeedUri);
            return Intent.createChooser(intent, "Edit with");
        }

        @Override
        public void handleResult(int resultCode, final Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Uri result = data.getData();
                            Uri stored = ContentCorral.copyContent(mContext, result);
                            if (stored == null) {
                                Log.w(TAG, "Error storing content in corral");
                                stored = result;
                            }
                            DbObject outboundObj = PictureObj.from(mContext, stored);
                            try {
                                outboundObj.getJson().put(DbObjects.TARGET_HASH, mHash);
                                outboundObj.getJson().put(DbObjects.TARGET_RELATION, DbRelation.RELATION_EDIT);
                            } catch (JSONException e) {}
                            Helpers.sendToFeed(mContext, outboundObj, mFeedUri);
                        } catch (IOException e) {
                            Log.e(HomeActivity.TAG, "Error reading photo data.", e);
                            toast("Error reading photo data.");
                        }
                    }
                }.start();
            }
        }

        private final void toast(final String text) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
