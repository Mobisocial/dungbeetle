package edu.stanford.mobisocial.dungbeetle.feed.presence;

import java.io.IOException;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class PhotosPresence extends FeedPresence {
    private static final String TAG = "livephotos";
    private boolean mSharePhotos = false;
    private PhotoContentObserver mPhotoObserver;

    @Override
    public String getName() {
        return "Photos";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mSharePhotos) {
            if (getFeedsWithPresence().size() == 0) {
                Toast.makeText(context, "No longer sharing photos", Toast.LENGTH_SHORT).show();
                context.getContentResolver().unregisterContentObserver(mPhotoObserver);
                mSharePhotos = false;
                mPhotoObserver = null;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                mSharePhotos = true;
                mPhotoObserver = new PhotoContentObserver(context);
                context.getContentResolver().registerContentObserver(
                        Images.Media.EXTERNAL_CONTENT_URI, true, mPhotoObserver);
                Toast.makeText(context, "Now sharing new photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class PhotoContentObserver extends ContentObserver {
        private final Context mmContext;
        private Uri mLastShared;

        public PhotoContentObserver(Context context) {
            super(new Handler(context.getMainLooper()));
            mmContext = context;
        }

        public void onChange(boolean selfChange) {
            if (mSharePhotos) {
                try {
                    Uri photo = getLatestCameraPhoto();
                    if (photo == null || photo.equals(mLastShared)) {
                        return;
                    }
                    mLastShared = photo;
                    DbObject obj = PictureObj.from(mmContext, photo);
                    for (Uri uri : getFeedsWithPresence()) {
                        Helpers.sendToFeed(mmContext, obj, uri);
                    }
                } catch (IOException e) {}
            }
        };

        private Uri getLatestCameraPhoto() {
            String selection = ImageColumns.BUCKET_DISPLAY_NAME + " = 'Camera'";
            String[] selectionArgs = null;
            String sort = ImageColumns.DATE_TAKEN + " desc";
            Cursor c =
                android.provider.MediaStore.Images.Media.query(mmContext.getContentResolver(),
                        Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { ImageColumns._ID }, selection, selectionArgs, sort );

            int idx = c.getColumnIndex(ImageColumns._ID);
            if (c.moveToFirst()) {
                return Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, c.getString(idx));
            }
            return null;
        }
    };
}
