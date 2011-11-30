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
import org.mobisocial.corral.CorralClient;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import edu.stanford.mobisocial.dungbeetle.obj.action.EditPhotoAction;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class ImageGalleryActivity extends FragmentActivity implements LoaderCallbacks<Cursor>,
        InstrumentedActivity {
    private static final String TAG = "imageGallery";
    private static final boolean DBG = false;

	private final String extStorageDirectory =
	        Environment.getExternalStorageDirectory().toString() + "/MusubiPictures/";
	private Gallery mGallery;
	private ImageGalleryAdapter mAdapter;
	private Uri mFeedUri;
	private long mInitialObjId;
	private int mInitialSelection = -1;
	private CorralClient mCorralClient;

	String mSelection = "type = ?";
    String[] mSelectionArgs = new String[] { PictureObj.TYPE };
    String mSortOrder = DbObject._ID + " DESC";

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mCorralClient = CorralClient.getInstance(this);
        mFeedUri = getIntent().getData();
        long hash = getIntent().getLongExtra("objHash", -1);
        mInitialObjId = App.instance().getMusubi().objForHash(hash).getLocalId();

        getSupportLoaderManager().initLoader(0, null, this);
        mGallery = new SlowGallery(this);
        mGallery.setBackgroundColor(Color.BLACK);
        addContentView(mGallery, CommonLayouts.FULL_SCREEN);
        if (savedInstanceState != null) {
            mInitialSelection = savedInstanceState.getInt("selection");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selection", mGallery.getSelectedItemPosition());
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

    private static class ImageGalleryAdapter extends CursorAdapter {
        private final Context mContext;
        private final int mInitialSelection;
        private final int COL_JSON;
        private final int COL_ID;

        public int getInitialSelection() {
            return mInitialSelection;
        }

        private ImageGalleryAdapter(Context context, Cursor c, int init) {
            super(context, c);
            mContext = context;
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
                if (json.has(CorralClient.OBJ_LOCAL_URI)) {
                    DbObj obj = App.instance().getMusubi().objForCursor(cursor);
                    new CorralLoaderThread((Activity)context, im, obj).start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "error loading json", e);
            }
            DbObj obj = App.instance().getMusubi().objForCursor(cursor);
            byte[] bytes = obj.getRaw();
            if (bytes == null) {
                Log.e(TAG, "Null image bytes for " + im.getTag());
                im.setImageResource(R.drawable.icon_full);
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            im.setImageBitmap(bitmap);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            ImageView im = new ImageView(mContext);
            im.setLayoutParams(new Gallery.LayoutParams(
                    Gallery.LayoutParams.MATCH_PARENT,
                    Gallery.LayoutParams.MATCH_PARENT));
            im.setScaleType(ImageView.ScaleType.FIT_CENTER);
            im.setBackgroundColor(Color.BLACK);
            return im;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                ImageView im = (ImageView) convertView;
                ((BitmapDrawable)im.getDrawable()).getBitmap().recycle();
            }
            return super.getView(position, convertView, parent);
        }
    }

    private static final int MENU_SAVE = 0;
    private static final int MENU_SET_PROFILE = 1;
    private static final int MENU_EDIT = 2;

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, MENU_EDIT, 0, "Edit");
        menu.add(0, MENU_SAVE, 0, "Save to SD Card");
        menu.add(0, MENU_SET_PROFILE, 0, "Set as Profile");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE: {
                long objId = (Long)mGallery.getSelectedView().getTag();
                DbObj obj = App.instance().getMusubi().objForId(objId);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                Date date = new Date();
                File file = new File(extStorageDirectory, dateFormat.format(date) + ".jpg");
                File fileDirectory = new File(extStorageDirectory);
                fileDirectory.mkdirs();

                if (mCorralClient.fileAvailableLocally(obj)) {
                    try {
                        Uri fileUri = mCorralClient.fetchContent(obj);
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
            case MENU_SET_PROFILE: {
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
            case MENU_EDIT: {
                long objId = (Long)mGallery.getSelectedView().getTag();
                DbObj obj = App.instance().getMusubi().objForId(objId);
                doActivityForResult(new EditPhotoAction.EditCallout(this, obj));
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
        final CorralClient mCorralClient;

        public CorralLoaderThread(Activity context, ImageView imageView, DbObj obj) {
            mObj = obj;
            mImageView = imageView;
            mContext = context;
            mCorralClient = CorralClient.getInstance(context);
        }

        public void run() {
            try {
                final Uri fileUri = mCorralClient.fetchContent(mObj);
                JSONObject json = mObj.getJson();
                if (fileUri == null) {
                    try {
                        Log.d(TAG, "Failed to go HD for " +
                                json.getString(CorralClient.OBJ_LOCAL_URI));
                    } catch (JSONException e) {
                        Log.d(TAG, "Failed to go HD for " + json);
                    }
                    return;
                }
                if (DBG) Log.d(TAG, "Opening HD file " + fileUri);

                int numBytes = getNumBytes(mContext, fileUri);
                InputStream is = mContext.getContentResolver().openInputStream(fileUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (numBytes > 256*1024) {
                    if (DBG) Log.d(TAG, "Resizing image of size " + numBytes);
                    options.inSampleSize = 4;
                } else {
                    if (DBG) Log.d(TAG, "Not resizing image of size " + numBytes);
                }

                Matrix matrix = new Matrix();
                float rotation = PhotoTaker.rotationForImage(mContext, fileUri);
                if (rotation != 0f) {
                    matrix.preRotate(rotation);
                }
                bitmap = BitmapFactory.decodeStream(is, null, options);

                if (bitmap == null) {
                    Log.w(TAG, "failed to decode bitmap from " + fileUri);
                    return;
                }

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                if (DBG) Log.d(TAG, "created new bitmap, binding to view");
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
        }

        private int getNumBytes(Activity context, Uri fileUri) throws IOException {
            InputStream in = context.getContentResolver().openInputStream(fileUri);
            byte[] buff = new byte[1024];
            int r;
            int total = 0;
            while ((r = in.read(buff)) > 0) {
                total += r;
            }
            in.close();
            return total;
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, mFeedUri, null, mSelection, mSelectionArgs, mSortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mAdapter == null) {
            String[] projection = new String[] { DbObj.COL_ID };
            Cursor hashes = getContentResolver().query(
                    mFeedUri, projection, mSelection, mSelectionArgs, mSortOrder);
            int init = binarySearch(hashes, mInitialObjId, 0);
            mAdapter = new ImageGalleryAdapter(this, cursor, init);
            mGallery.setAdapter(mAdapter);
            mGallery.setSelection((mInitialSelection == -1)
                    ? mAdapter.getInitialSelection() : mInitialSelection);
        } else {
            mAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }

    private static int REQUEST_ACTIVITY_CALLOUT = 39;
    private static ActivityCallout mCurrentCallout;

    @Override
    public void showDialog(Dialog dialog) {
        dialog.show(); // TODO: Figure out how to preserve dialog during screen rotation.
    }

    public void doActivityForResult(ActivityCallout callout) {
        mCurrentCallout = callout;
        Intent launch = callout.getStartIntent();
        if(launch != null)
            startActivityForResult(launch, REQUEST_ACTIVITY_CALLOUT);
        else {
            Log.wtf(callout.getClass().getCanonicalName(), "I failed to return a valid intent, so something is probably very bad.");
            Toast.makeText(this, "Callback for object type failed! " + callout.getClass().getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTIVITY_CALLOUT) {
            mCurrentCallout.handleResult(resultCode, data);
        }
    }
}