package edu.stanford.mobisocial.dungbeetle.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

public class PhotoTaker implements ActivityCallout {
    private static final String TAG = "phototaker";
	private final ResultHandler mResultHandler;
	private final Context mContext;
	private final boolean mSnapshot;
	private final int mSize;

	public PhotoTaker(Context c, ResultHandler handler, int size, boolean snapshot) {
		mContext = c;
		mResultHandler = handler;
        mSnapshot = snapshot;
        mSize = size;
	}

	public PhotoTaker(Context c, ResultHandler handler) {
        this(c, handler, 80, false);
	}

	@Override
	public Intent getStartIntent() {
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(getTempFile(mContext)));
		return intent;
	}

	@Override
	public void handleResult(int resultCode, Intent resultData) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}

		final File file;
		final File path = new File(Environment.getExternalStorageDirectory(),
                                   mContext.getPackageName());
		if (!path.exists()) {
			path.mkdir();
		}

		file = new File(path, "image.tmp");
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 8;
			Bitmap sourceBitmap = BitmapFactory.decodeFile(file.getPath(),
                                                           options);
			// Bitmap sourceBitmap = Media.getBitmap(getContentResolver(),
			// Uri.fromFile(file) );
			int width = sourceBitmap.getWidth();
			int height = sourceBitmap.getHeight();
			int cropSize = Math.min(width, height);

			int targetSize = mSize;
			float scaleSize = ((float) targetSize) / cropSize;

			Matrix matrix = new Matrix();
			try {
			    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
                matrix.preRotate(rotation);
			} catch (IOException e) {
			    Log.e(TAG, "Error checking exif", e);
			}
			matrix.postScale(scaleSize, scaleSize);
            Bitmap resizedBitmap;
            if(mSnapshot) {
                resizedBitmap = Bitmap.createBitmap(
                        sourceBitmap, 0, 0,width, height, matrix, true);
            } else {
                Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 0, 0, cropSize, cropSize);
    			resizedBitmap = Bitmap.createBitmap(
    			        cropped, 0, 0, cropSize, cropSize, matrix, true);
            }

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			byte[] data = baos.toByteArray();

			mResultHandler.onResult(data);
		} catch (Exception e) {

		}
	}

	public interface ResultHandler {
		public void onResult(byte[] data);
	}

	private static File getTempFile(Context context) {
		// it will return /sdcard/image.tmp
		final File path = new File(Environment.getExternalStorageDirectory(),
                                   context.getPackageName());
		if (!path.exists()) {
			path.mkdir();
		}
		return new File(path, "image.tmp");
	}

	public static float rotationForImage(Context context, Uri uri) {
	    if (uri.getScheme().equals("content")) {
            String[] projection = { Images.ImageColumns.ORIENTATION };
            Cursor c = context.getContentResolver().query(
                    uri, projection, null, null, null);
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } else if (uri.getScheme().equals("file")) {
            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                int rotation = (int) PhotoTaker.exifOrientationToDegrees(
                        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL));
                return rotation;
            } catch (IOException e) {
                Log.e(TAG, "Error checking exif", e);
            }
        }
	    return 0f;
	}

	private static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}
