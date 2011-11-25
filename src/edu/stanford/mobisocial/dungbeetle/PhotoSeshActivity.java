package edu.stanford.mobisocial.dungbeetle;

import java.io.IOException;

import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.View;
import android.widget.Button;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * A collaborative photo capturing application. Captures PictureObj data
 * and stores it in a Musubi subfeed.
 *
 */
public class PhotoSeshActivity extends Activity {
    private int PHOTO_SESH_ID = 3;
    private Musubi mMusubi;
    private NotificationManager mNotificationManager;

    /** TODO: move to service **/
    private static boolean mSharePhotos;
    private static PhotoContentObserver mPhotoObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_sesh);
        mMusubi = Musubi.getInstance(this, getIntent());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mPhotoObserver = new PhotoContentObserver(PhotoSeshActivity.this);
        Button button = (Button)(findViewById(R.id.photo_sesh));
        button.setOnClickListener(mStartSesh);
        if (mSharePhotos) {
            button.setText("Leave photo sesh");
        } else {
            button.setText("Join photo sesh");
        }
    }

    private final View.OnClickListener mStartSesh = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button button = (Button)(findViewById(R.id.photo_sesh));
            if (mSharePhotos) {
                disableSharePhotos();
                button.setText("Join photo sesh");
            } else {
                enableSharePhotos();
                button.setText("Leave photo sesh");
            }
            mSharePhotos = !mSharePhotos;
        }
        
    };

    private void disableSharePhotos() {
        mNotificationManager.cancelAll();
        getContentResolver().unregisterContentObserver(mPhotoObserver);
    }

    private void enableSharePhotos() {
        /** Listen for new photos **/
        getContentResolver().registerContentObserver(
                Images.Media.EXTERNAL_CONTENT_URI, true, mPhotoObserver);

        int icon = android.R.drawable.ic_menu_camera;
        CharSequence tickerText = "New photo sesh.";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);

        /** User notification **/
        CharSequence contentTitle = "Photo Sesh";
        CharSequence contentText = "Sharing photos with " +
                mMusubi.getFeed().getRemoteUsers().size() + " people.";
        Intent notificationIntent = new Intent(PhotoSeshActivity.this, PhotoSeshActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(PhotoSeshActivity.this,
                0, notificationIntent, 0);

        notification.setLatestEventInfo(PhotoSeshActivity.this, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(PHOTO_SESH_ID, notification);
    }


    class PhotoContentObserver extends ContentObserver {
        private final Context mmContext;
        private Uri mLastShared;

        public PhotoContentObserver(Context context) {
            super(new Handler(context.getMainLooper()));
            mmContext = context;
        }

        /**
         * A new photo has been detected in the media store.
         */
        public void onChange(boolean selfChange) {
            if (mSharePhotos) {
                try {
                    Uri photo = getLatestCameraPhoto();
                    if (photo == null || photo.equals(mLastShared)) {
                        return;
                    }

                    /** Put in the SocialDB **/
                    mLastShared = photo;
                    DbObject obj = PictureObj.from(mmContext, photo);
                    mMusubi.getObj().getSubfeed().postObj(obj);

                    /** TODO: Put in the Corral **/
                    // (currently handled in PictureObj)
                } catch (IOException e) {}
            }
        };

        private Uri getLatestCameraPhoto() {
            String selection = ImageColumns.BUCKET_DISPLAY_NAME + " = 'Camera'";
            String[] selectionArgs = null;
            String sort = ImageColumns._ID + " DESC LIMIT 1";
            Cursor c =
                    android.provider.MediaStore.Images.Media.query(mmContext.getContentResolver(),
                            Images.Media.EXTERNAL_CONTENT_URI,
                            new String[] { ImageColumns._ID }, selection, selectionArgs, sort );
            try {

                int idx = c.getColumnIndex(ImageColumns._ID);
                if (c.moveToFirst()) {
                    return Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, c.getString(idx));
                }
                return null;
            } finally {
                c.close();
            }
        }
    };
}