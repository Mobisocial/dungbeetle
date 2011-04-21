package edu.stanford.mobisocial.dungbeetle.util;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.objects.ProfilePictureObj;

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
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import android.util.Base64;

public class PhotoTaker implements ActivityCallout {
		private final ResultHandler mResultHandler;
		private final Context mContext;

		public PhotoTaker(Context c, ResultHandler handler) {
			mContext = c;
			mResultHandler = handler;
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

                mResultHandler.onResult(values);
            } catch (Exception e) {
            	
            }
    	}
    	
    	public interface ResultHandler {
    		public void onResult(ContentValues values);
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