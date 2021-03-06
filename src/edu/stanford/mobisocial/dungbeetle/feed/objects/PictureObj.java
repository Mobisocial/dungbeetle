/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.feed.objects;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;
import org.mobisocial.corral.CorralClient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.ImageGalleryActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.OutgoingMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class PictureObj extends DbEntryHandler
        implements FeedRenderer, Activator, UnprocessedMessageHandler, OutgoingMessageHandler {
	public static final String TAG = "PictureObj";

    public static final String TYPE = "picture";
    public static final String DATA = "data";

    // TODO: This is a hack, with many ways to fix. For example,
    // it can be used with its timestamp and an instance variable to
    // track a users' latest ip address.
    // Security should also be considered.

    @Override
    public String getType() {
        return TYPE;
    }

    /** 
     * This does NOT do any SCALING!
     */
    public static DbObject from(byte[] data) {
        return new DbObject(TYPE, new JSONObject(), data);
    }

    public static DbObject from(JSONObject base, byte[] data) {
        return new DbObject(TYPE, base, data);
    }

	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		byte[] raw = FastBase64.decode(json.optString(DATA));
		json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, raw);
	}

    public static DbObject from(Context context, Uri imageUri) throws IOException {
        // Query gallery for camera picture via
        // Android ContentResolver interface
        ContentResolver cr = context.getContentResolver();
        
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(cr.openInputStream(imageUri), null, options);

		int targetSize = 200;
		int xScale = (options.outWidth  + targetSize - 1) / targetSize;
		int yScale = (options.outHeight + targetSize - 1) / targetSize;
		
		int scale = xScale < yScale ? xScale : yScale;
		//uncomment this to get faster power of two scaling
		//for(int i = 0; i < 32; ++i) {
		//	int mushed = scale & ~(1 << i);
		//	if(mushed != 0)
		//		scale = mushed;
		//}
		
		options.inJustDecodeBounds = false;
		options.inSampleSize = scale;
		
		Bitmap sourceBitmap = BitmapFactory.decodeStream(cr.openInputStream(imageUri), null, options);

        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        int cropSize = Math.min(width, height);

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
        byte[] data = baos.toByteArray();
        sourceBitmap.recycle();
        sourceBitmap = null;
        resizedBitmap.recycle();
        resizedBitmap = null;
        System.gc(); // TODO: gross.

        JSONObject base = new JSONObject();
        if (ContentCorral.CONTENT_CORRAL_ENABLED) {
            try {
                String type = cr.getType(imageUri);
                if (type == null) {
                    type = "image/jpeg";
                }
                base.put(CorralClient.OBJ_LOCAL_URI, imageUri.toString());
                base.put(CorralClient.OBJ_MIME_TYPE, type);
                String localIp = ContentCorral.getLocalIpAddress();
                if (localIp != null) {
                    base.put(Contact.ATTR_LAN_IP, localIp);
                }
            } catch (JSONException e) {
                Log.e(TAG, "impossible json error possible!");
            }
        }
        return from(base, data);
    }

	public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
	    JSONObject content = obj.getJson();
        byte[] raw = obj.getRaw();
		if (raw == null) {
			Pair<JSONObject, byte[]> p = splitRaw(content);
			content = p.first;
			raw = p.second;
		}
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setImageBitmap(BitmapFactory.decodeByteArray(raw, 0, raw.length));
//        App.instance().objectImages.lazyLoadImage(raw.hashCode(), raw, imageView);
        frame.addView(imageView);
	}
	public Pair<JSONObject, byte[]> handleUnprocessed(Context context,
			JSONObject msg) {
	    if (!msg.has(DATA)) {
	            return null;
	    }

	    byte[] bytes = FastBase64.decode(msg.optString(DATA));
        msg.remove(DATA);
		return new Pair<JSONObject, byte[]>(msg, bytes);
	}

	@Override
    public void activate(Context context, SignedObj obj) {
	    // TODO: set data uri for obj
	    Intent intent = new Intent(context, ImageGalleryActivity.class);
	    intent.setData(Feed.uriForName(obj.getFeedName()));
	    intent.putExtra("objHash", obj.getHash());
	    if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
	    context.startActivity(intent);
    }

	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		try {
			if(raw != null)
				objData = objData.put(DATA, FastBase64.encodeToString(raw));
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
        byte[] bytes = FastBase64.decode(json.optString(DATA));
        json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, bytes);
	}
}
