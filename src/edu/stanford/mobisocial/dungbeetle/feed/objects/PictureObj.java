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
import edu.stanford.mobisocial.dungbeetle.feed.iface.OutgoingMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
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
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import android.util.Log;
import android.util.Pair;

public class PictureObj implements DbEntryHandler, FeedRenderer, Activator, UnprocessedMessageHandler, OutgoingMessageHandler {
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
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		byte[] raw = Base64.decode(json.optString(DATA));
		json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, raw);
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
        System.gc();
        return from(data);
    }

    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, false);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
        }catch(JSONException e){}
        return obj;
    }
	
	public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
		if(raw == null) {
			Pair<JSONObject, byte[]> p = splitRaw(content);
			content = p.first;
			raw = p.second;
		}
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        BitmapFactory bf = new BitmapFactory();
        imageView.setImageBitmap(bf.decodeByteArray(raw, 0, raw.length));
//        App.instance().objectImages.lazyLoadImage(raw.hashCode(), raw, imageView);
        frame.addView(imageView);
	}
	public Pair<JSONObject, byte[]> handleUnprocessed(Context context,
			JSONObject msg) {
        byte[] bytes = Base64.decode(msg.optString(DATA));
        msg.remove(DATA);
		return new Pair<JSONObject, byte[]>(msg, bytes);
	}

	@Override
    public void activate(Context context, JSONObject content, byte[] raw){
		if(raw == null)
	        raw = Base64.decode(content.optString(DATA));
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("bytes", raw);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent); 
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		try {
			if(raw != null)
				objData = objData.put(DATA, Base64.encodeToString(raw, false));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return objData;
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

	@Override
	public Pair<JSONObject, byte[]> handleOutgoing(JSONObject json) {
        byte[] bytes = Base64.decode(json.optString(DATA));
        json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, bytes);
	}
}
