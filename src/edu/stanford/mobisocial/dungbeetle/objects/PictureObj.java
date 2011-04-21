package edu.stanford.mobisocial.dungbeetle.objects;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.json.JSONException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.ObjectsActivity.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Object;

import android.util.Base64;

public class PictureObj implements IncomingMessageHandler, FeedRenderer {
	public static final String TAG = "PictureObj";

    public static final String TYPE = "picture";
    public static final String DATA = "data";

        
    public static JSONObject json(String data){
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", data);
            
        }catch(JSONException e){}
        return obj;
    }

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		byte[] data = Base64.decode(obj.optString(DATA), Base64.DEFAULT);
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.PICTURE, data);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
	}

	public boolean willRender(JSONObject object) { 
		return willHandle(null, object);
	}
	
	
	// TODO
	public void render(Context context, ViewGroup frame) {
		
	}

	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
		byte[] data = Base64.decode(content.optString(DATA), Base64.DEFAULT);
		imageView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(imageView);
	}
	
	public static class PhotoTaker implements ActivityCallout {
		private final Context mContext;

		public PhotoTaker(Context c) {
			mContext = c;
		}
    	@Override
    	public Intent getStartIntent() {
    		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(mContext)) ); 
            return intent;
    	}
    	
    	@Override
    	public void handleResult(int resultCode, Intent resultData) {
    		if (resultCode != Activity.RESULT_OK) {
    			return;
    		}

    		final File file;
        	final File path = new File( Environment.getExternalStorageDirectory(), mContext.getPackageName() );
            if(!path.exists()){
                path.mkdir();
            }
            file = new File(path, "image.tmp");
            try {
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize = 8;
                Bitmap sourceBitmap=BitmapFactory.decodeFile(file.getPath(),options);

                
                //Bitmap sourceBitmap = Media.getBitmap(getContentResolver(), Uri.fromFile(file) );
                int width = sourceBitmap.getWidth();
                int height = sourceBitmap.getHeight();
                int cropSize = Math.min(width, height);
                Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 0, 0, cropSize, cropSize);

                int targetSize = 80;
                float scaleSize = ((float) targetSize) / cropSize;
                Matrix matrix = new Matrix();
                // resize the bit map
                matrix.postScale(scaleSize, scaleSize);
                matrix.postRotate(270);

                // recreate the new Bitmap
                Bitmap resizedBitmap = Bitmap.createBitmap(cropped, 0, 0, 
                                  cropSize, cropSize, matrix, true);
                
                final ImageView icon = new ImageView(mContext);
                icon.setImageBitmap(resizedBitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object   
                byte[] data = baos.toByteArray(); 

                ContentValues values = new ContentValues();
                String encoded = Base64.encodeToString(data, Base64.DEFAULT);
                JSONObject obj = ProfilePictureObj.json(encoded);
                values.put(Object.JSON, obj.toString());
                values.put(Object.TYPE, ProfilePictureObj.TYPE);

                Helpers.sendToFeed(mContext, values);
            } catch (Exception e) {
            	
            }
    	}
    	
    	private static File getTempFile(Context context){
            //it will return /sdcard/image.tmp
            final File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );
            if(!path.exists()){
                path.mkdir();
            }
            return new File(path, "image.tmp");
        }
    }
}
