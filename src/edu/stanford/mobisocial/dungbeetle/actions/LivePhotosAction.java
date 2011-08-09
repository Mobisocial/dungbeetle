package edu.stanford.mobisocial.dungbeetle.actions;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.actions.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.objects.PictureObj;

public class LivePhotosAction implements FeedAction {
    private static final String TAG = "livephotos";
    private boolean mSharePhotos = false;
    private Context mContext;
    private Uri mFeedUri;

    @Override
    public String getName() {
        return "Live Photos";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        boolean sharePhotos = !mSharePhotos;
        synchronized(this) {
            mFeedUri = feedUri;
            mContext = context.getApplicationContext();
            mSharePhotos = sharePhotos;
        }

        if (sharePhotos) {
            IntentFilter iF = new IntentFilter();
            iF.addAction("com.android.camera.NEW_PICTURE");
            try {
                iF.addDataType("*/*");
            } catch (MalformedMimeTypeException e) {
                Log.wtf(TAG, "Bad mime", e);
            }
            mContext.registerReceiver(mReceiver, iF);

            Toast.makeText(context, "Now sharing new photos", Toast.LENGTH_SHORT).show();
        } else {
            mContext.unregisterReceiver(mReceiver);
            Toast.makeText(context, "No longer sharing photos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSharePhotos) {
                try {
                    Helpers.sendToFeed(mContext, pictureFromUri(context, intent.getData()), mFeedUri);
                } catch (IOException e) {}
            }
        }
    };

    private DbObject pictureFromUri(Context context, Uri img) throws FileNotFoundException {
        Bitmap sourceBitmap = BitmapFactory.decodeStream(
                context.getContentResolver().openInputStream(img));

        // Bitmap sourceBitmap = Media.getBitmap(getContentResolver(),
        // Uri.fromFile(file) );
        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        int cropSize = Math.min(width, height);
        Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 
                                             0, 0, 
                                             cropSize,
                                             cropSize);
    
        int targetSize = 200;
        float scaleSize = ((float) targetSize) / cropSize;
    
        Matrix matrix = new Matrix();
        matrix.postScale(scaleSize, scaleSize);
        Bitmap resizedBitmap = Bitmap.createBitmap(cropped, 0, 0, width,
                height, matrix, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        return PictureObj.from(data);
    }
}
