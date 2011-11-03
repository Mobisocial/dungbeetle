package edu.stanford.mobisocial.dungbeetle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CursorAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class ImageGalleryActivity extends Activity {
    private static final String TAG = "imageGallery";

	private final String extStorageDirectory =
	        Environment.getExternalStorageDirectory().toString() + "/MusubiPictures/";
	private Gallery mGallery;
	private ImageGalleryAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setContentView(R.layout.image_gallery);

        Uri feedUri = getIntent().getData();
        long objHash = getIntent().getLongExtra("objHash", -1);
        mAdapter = ImageGalleryAdapter.forObj(this, feedUri, objHash);
        
        //mGallery = (Gallery)findViewById(R.id.gallery);
        mGallery = new SlowGallery(this);
        mGallery.setBackgroundColor(Color.BLACK);
        addContentView(mGallery, CommonLayouts.FULL_SCREEN);
        mGallery.setAdapter(mAdapter);
        if (savedInstanceState != null) {
            mGallery.setSelection(savedInstanceState.getInt("selection"));
        } else {
            mGallery.setSelection(mAdapter.getInitialSelection());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selection", mGallery.getSelectedItemPosition());
    }

    private static class ImageGalleryAdapter extends CursorAdapter {
        private final int mInitialSelection;
        private final int COL_JSON;
        private final int COL_ID;

        public static ImageGalleryAdapter forObj(Context context, Uri feedUri, long objHash) {
            String selection = "type = ?";
            String[] selectionArgs = new String[] { PictureObj.TYPE };
            Cursor c = context.getContentResolver().query(feedUri, null,
                    selection, selectionArgs, DbObject._ID + " DESC");

            DbObj obj = App.instance().getMusubi().objForHash(objHash);
            if (obj == null || !c.moveToFirst()) {
                Log.w(TAG, "Could not find image for viewing");
                return null;
            }

            int colId = c.getColumnIndexOrThrow(DbObject._ID);
            long objId = obj.getLocalId();
            int init = binarySearch(c, objId, colId);
            return new ImageGalleryAdapter(context, c, init);
        }

        public int getInitialSelection() {
            return mInitialSelection;
        }

        // Cursor must be ordered DESC.
        // The sort order and search order are opposite!
        private static int binarySearch(Cursor c, long id, int colId) {
            long test;
            int first = 0;
            int max = c.getCount();
            while (first < max) {
                int mid = (first + max) / 2;
                c.moveToPosition(mid);
                test = c.getLong(colId);
                if (id > test) {
                    max = mid;
                } else if (id < test) {
                    first = mid + 1;
                } else {
                    return mid;
                }
            }
            return 0;
        }

        private ImageGalleryAdapter(Context context, Cursor c, int init) {
            super(context, c);
            mInitialSelection = init;
            COL_JSON = c.getColumnIndexOrThrow(DbObject.JSON);
            COL_ID = c.getColumnIndexOrThrow(DbObject._ID);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView im = (ImageView)view;
            im.setTag(cursor.getLong(COL_ID));
            try {
                JSONObject json = new JSONObject(cursor.getString(COL_JSON));
                if (json.has(ContentCorral.OBJ_LOCAL_URI)) {
                    DbObj obj = App.instance().getMusubi().objForCursor(cursor);
                    new CorralLoaderThread((Activity)context, im, obj).start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "error loading json", e);
            }
            DbObj obj = App.instance().getMusubi().objForCursor(cursor);
            byte[] bytes = obj.getRaw();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            im.setImageBitmap(bitmap);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            ImageView im = new ImageView(context);
            im.setLayoutParams(new Gallery.LayoutParams(
                    Gallery.LayoutParams.MATCH_PARENT,
                    Gallery.LayoutParams.MATCH_PARENT));
            im.setScaleType(ImageView.ScaleType.FIT_CENTER);
            im.setBackgroundColor(Color.BLACK);
            return im;
        }
    }

    private final static int SAVE = 0;
    private final static int SET_PROFILE = 1;

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, SAVE, 0, "Download to SD Card");
        menu.add(0, SET_PROFILE, 0, "Set as Profile");
        //menu.add(1, ANON, 1, "Add anon profile");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SAVE: {
                long objId = (Long)mGallery.getSelectedView().getTag();
                DbObj obj = App.instance().getMusubi().objForId(objId);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                Date date = new Date();
                File file = new File(extStorageDirectory, dateFormat.format(date) + ".jpg");
                File fileDirectory = new File(extStorageDirectory);
                fileDirectory.mkdirs();

                if (ContentCorral.fileAvailableLocally(this, obj)) {
                    try {
                        Uri fileUri = ContentCorral.fetchContent(this, obj);
                        InputStream is = this.getContentResolver().openInputStream(fileUri);
                        FileOutputStream out = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int r;
                        while ((r = is.read(buf)) > 0) {
                            out.write(buf, 0, r);
                        }
                        toast("Saved image to SD card.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error transferring file", e);
                        toast(e.getMessage());

                        if (file != null && file.exists()) {
                            file.delete();
                        }
                    }
                } else {
                    try {
                        byte[] picBytes = obj.getRaw();
                        OutputStream outStream = new FileOutputStream(file);
                        outStream.write(picBytes);
                        outStream.flush();
                        outStream.close();
                        toast("Saved image to SD card.");
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Error saving file", e);
                        toast(e.toString());
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving file", e);
                        toast(e.toString());
                    }
                }
                return true;
            }
            case SET_PROFILE: {
                new Thread() {
                    public void run() {
                        long objId = (Long)mGallery.getSelectedView().getTag();
                        DbObj obj = App.instance().getMusubi().objForId(objId);
                        byte[] picBytes = obj.getRaw();
                        Helpers.updatePicture(ImageGalleryActivity.this, picBytes);
                        toast("Set profile picture.");
                    };
                }.start(); 
                return true;
            }
            default:
                return false;
        }
    }

    public class SlowGallery extends Gallery {
        public SlowGallery(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public SlowGallery(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public SlowGallery(Context context) {
            super(context);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX / 3, velocityY);
        }
    }

    private static class CorralLoaderThread extends Thread {
        final DbObj mObj;
        final ImageView mImageView;
        final Activity mContext;
        Bitmap bitmap;

        public CorralLoaderThread(Activity context, ImageView imageView, DbObj obj) {
            mObj = obj;
            mImageView = imageView;
            mContext = context;
        }

        public void run() {
            try {
                final Uri fileUri = ContentCorral.fetchContent(mContext, mObj);
                JSONObject json = mObj.getJson();
                if (fileUri == null) {
                    try {
                        Log.d(TAG, "Failed to go HD for " +
                                json.getString(ContentCorral.OBJ_LOCAL_URI));
                    } catch (JSONException e) {
                        Log.d(TAG, "Failed to go HD for " + json);
                    }
                    return;
                }
                // Log.d(TAG, "Opening HD file " + fileUri);

                InputStream is = mContext.getContentResolver().openInputStream(fileUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;

                Matrix matrix = new Matrix();
                float rotation = PhotoTaker.rotationForImage(mContext, fileUri);
                if (rotation != 0f) {
                    matrix.preRotate(rotation);
                }
                bitmap = BitmapFactory.decodeStream(is, null, options);

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                if ((Long)mImageView.getTag() == mObj.getLocalId()) {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ((Long)mImageView.getTag() == mObj.getLocalId()) {
                                mImageView.setImageBitmap(bitmap);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to get hd content", e);
            }
        };
    }

    private final void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImageGalleryActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}