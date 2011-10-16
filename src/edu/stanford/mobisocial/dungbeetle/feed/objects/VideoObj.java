package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.User;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.OutgoingMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

public class VideoObj extends DbEntryHandler
        implements FeedRenderer, Activator, UnprocessedMessageHandler, OutgoingMessageHandler {
	public static final String TAG = "VideoObj";

    public static final String TYPE = "video";
    public static final String DATA = "data";

    public static final String MIME_TYPE = "mimeType";
    public static final String LOCAL_URI = "localUri";

    @Override
    public String getType() {
        return TYPE;
    }

    /** 
     * This does NOT do any SCALING!
     */
    public static DbObject from(byte[] data) {
        return new DbObject(TYPE, VideoObj.json(data));
    }

    public static DbObject from(JSONObject base, byte[] data) {
        return new DbObject(TYPE, VideoObj.json(base, data));
    }
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		byte[] raw = Base64.decode(json.optString(DATA));
		json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, raw);
	}

    public static DbObject from(Context context, Uri videoUri) throws IOException {
        // Query gallery for camera picture via
        // Android ContentResolver interface
        ContentResolver cr = context.getContentResolver();
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize = 1;
        long videoId = Long.parseLong(videoUri.getLastPathSegment());
        Bitmap curThumb = MediaStore.Video.Thumbnails.getThumbnail(
                cr, videoId, MediaStore.Video.Thumbnails.MINI_KIND, options);
        int targetSize = 200;
        int width = curThumb.getWidth();
        int height = curThumb.getHeight();
        int cropSize = Math.min(width, height);
        float scaleSize = ((float) targetSize) / cropSize;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleSize, scaleSize);
        curThumb = Bitmap.createBitmap(
                curThumb, 0, 0,width, height, matrix, true);
        JSONObject base = new JSONObject();
        String localIp = ContentCorral.getLocalIpAddress();
        if (localIp != null) {
            try {
                // TODO: Security breach hack?
                base.put(Contact.ATTR_LAN_IP, localIp);
                base.put(LOCAL_URI, videoUri.toString());
                base.put(MIME_TYPE, cr.getType(videoUri));
            } catch (JSONException e) {
                Log.e(TAG, "impossible json error possible!");
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        curThumb.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();
        return from(base, data);
    }

    public static JSONObject json(byte[] data){
        JSONObject obj = new JSONObject();
        return json(obj, data);
    }

    public static JSONObject json(JSONObject base, byte[] data){
        String encoded = Base64.encodeToString(data, false);
        try{
            base.put("data", encoded);
        }catch(JSONException e){}
        return base;
    }
	
	public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
	    JSONObject content = obj.getJson();
        byte[] raw = obj.getRaw();

		if(raw == null) {
			Pair<JSONObject, byte[]> p = splitRaw(content);
			content = p.first;
			raw = p.second;
		}

		LinearLayout inner = new LinearLayout(context);
		inner.setLayoutParams(CommonLayouts.FULL_WIDTH);
		inner.setOrientation(LinearLayout.HORIZONTAL);
		frame.addView(inner);

		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        BitmapFactory bf = new BitmapFactory();
        imageView.setImageBitmap(bf.decodeByteArray(raw, 0, raw.length));
        inner.addView(imageView);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(R.drawable.play);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        inner.addView(iconView);
	}
	public Pair<JSONObject, byte[]> handleUnprocessed(Context context,
			JSONObject msg) {
        byte[] bytes = Base64.decode(msg.optString(DATA));
        msg.remove(DATA);
		return new Pair<JSONObject, byte[]>(msg, bytes);
	}

	@Override
    public void activate(final Context context, final SignedObj obj) {
	    JSONObject content = obj.getJson();
	    if (ContentCorral.CONTENT_CORRAL_ENABLED) {
	        User sender = obj.getSender();
	        Log.d(TAG, "Corraling video");
	        if (ContentCorral.fileAvailableLocally(context, obj)) {
	            try {
	                Uri contentUri = ContentCorral.fetchContent(context, obj);
	                startViewer(context, contentUri);
                } catch (IOException e) {
                    Log.e(TAG, "The corral tricked me", e);
                }
	        } else {
	            Log.e(TAG, "trying to pull video");
	            new Thread() {
	                @Override
	                public void run() {
	                    try {
                            Uri contentUri = ContentCorral.fetchContent(context, obj);
                            startViewer(context, contentUri);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to corral", e);
                        }
	                }
	            }.start();
	        }
	    }
    }

	private void startViewer(Context context, Uri contentUri) {
	    Log.d(TAG, "Launching viewer for " + contentUri);
	    Intent i = new Intent(Intent.ACTION_VIEW);
        if (!(context instanceof Activity)) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        i.setDataAndType(contentUri, "video/*");
        context.startActivity(i);
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

	private final void toast(final Context context, final String text) {
	    ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT);
            }
        });
	}
}
