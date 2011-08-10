package edu.stanford.mobisocial.dungbeetle.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class PhotoTaker implements ActivityCallout {
    private static final String TAG = "phototaker";
	private final ResultHandler mResultHandler;
	private final Context mContext;
	@SuppressWarnings("unused")
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
			Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 0, 0, cropSize, cropSize);

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
