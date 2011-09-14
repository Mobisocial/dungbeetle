package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class PictureObj implements DbEntryHandler, FeedRenderer, Activator {
	public static final String TAG = "PictureObj";

    public static final String TYPE = "picture";
    public static final String DATA = "data";

    @Override
    public String getType() {
        return TYPE;
    }

    /** 
     * This does NOT do any SCALING!
     */
    public static DbObject from(byte[] data) {
        return new DbObject(TYPE, PictureObj.json(data));
    }

    public static DbObject from(Context context, Uri imageUri) throws IOException {
        // Query gallery for camera picture via
        // Android ContentResolver interface
        ContentResolver cr = context.getContentResolver();
        InputStream is = cr.openInputStream(imageUri);
        // Get binary bytes for encode
        byte[] data = getBytesFromFile(is);

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(
                data, 0, data.length, options);

        // Bitmap sourceBitmap = Media.getBitmap(getContentResolver(),
        // Uri.fromFile(file) );
        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        int cropSize = Math.min(width, height);

        int targetSize = 200;
        float scaleSize = ((float) targetSize) / cropSize;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleSize, scaleSize);
        float rotation = PhotoTaker.rotationForImage(context, imageUri);
        if (rotation != 0f) {
            matrix.preRotate(rotation);
        }

        Bitmap resizedBitmap = Bitmap.createBitmap(
                sourceBitmap, 0, 0, width, height, matrix, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        data = baos.toByteArray();
        sourceBitmap.recycle();
        resizedBitmap.recycle();
        return from(data);
    }

    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
        }catch(JSONException e){}
        return obj;
    }
	
	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        String bytes = content.optString(DATA);
        App.instance().objectImages.lazyLoadImage(bytes.hashCode(), bytes, imageView);
        frame.addView(imageView);
	}

	@Override
    public void activate(Context context, JSONObject content){
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("b64Bytes", bytes);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent); 
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {   
    }

    private static byte[] getBytesFromFile(InputStream is) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error reading bytes from file", e);
            return null;
        }
    }
}
