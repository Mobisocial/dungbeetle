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

import org.json.JSONException;
import org.json.JSONObject;
import org.mobisocial.corral.ContentCorral;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class ImageViewerActivity extends Activity {
    private static final String TAG = "imageViewer";
	private BitmapManager mgr = new BitmapManager(1);
	private ImageView im;

	private Bitmap bitmap;
	private final String extStorageDirectory =
	        Environment.getExternalStorageDirectory().toString() + "/MusubiPictures/";
	private Intent mIntent;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		im = (ImageView)findViewById(R.id.image);
		im.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mIntent = getIntent();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mIntent.hasExtra("image_url")){
            String url = mIntent.getStringExtra("image_url");
            ((App)getApplication()).objectImages.lazyLoadImage(
                url.hashCode(), Uri.parse(url), im);
            bitmap = mgr.getBitmap(url.hashCode(), url);
        }
        else if(mIntent.hasExtra("b64Bytes")){
            String b64Bytes = mIntent.getStringExtra("b64Bytes");
            ((App)getApplication()).objectImages.lazyLoadImage(
                b64Bytes.hashCode(), b64Bytes, im);
            bitmap = mgr.getBitmapB64(b64Bytes.hashCode(), b64Bytes);
        } else if(mIntent.hasExtra("bytes")){
            byte[] bytes = mIntent.getByteArrayExtra("bytes");
            ((App)getApplication()).objectImages.lazyLoadImage(
            		bytes.hashCode(), bytes, im);
            bitmap = mgr.getBitmap(bytes.hashCode(), bytes);
        } else if (mIntent.hasExtra("obj")) {
            try {
                final JSONObject content = new JSONObject(mIntent.getStringExtra("obj"));
                byte[] bytes = Base64.decode(content.optString(PictureObj.DATA));
                ((App)getApplication()).objectImages.lazyLoadImage(
                        bytes.hashCode(), bytes, im);
                bitmap = mgr.getBitmap(bytes.hashCode(), bytes);
            } catch (JSONException e) {}
        }
        
        if (mIntent.hasExtra("obj")) {
            try {
                final JSONObject content = new JSONObject(mIntent.getStringExtra("obj"));
                if (ContentCorral.ENABLE_CONTENT_CORRAL) {
                    if (content.has(PictureObj.LOCAL_IP)) {
                        // TODO: this is a proof-of-concept.
                        new Thread() {
                            public void run() {
                                try {
                                    if (!ContentCorral.fileAvailableLocally(ImageViewerActivity.this, content, "jpg")) {
                                        toast("Trying to go HD");
                                    }
                                    Log.d(TAG, "Trying to go HD...");
                                    final Uri fileUri = ContentCorral.fetchContent(
                                            ImageViewerActivity.this, content, "jpg");
                                    Log.d(TAG, "Opening HD file " + fileUri);

                                    // TODO: apply rotation.
                                    InputStream is = getContentResolver().openInputStream(fileUri);
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inSampleSize = 4;
                                    options.outHeight = im.getHeight();
                                    options.outWidth = im.getWidth();
                                    PhotoTaker.rotationForImage(ImageViewerActivity.this, fileUri);
                                    bitmap = BitmapFactory.decodeStream(is, null, options);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            im.setImageBitmap(bitmap);
                                        }
                                    });
                                } catch (IOException e) {
                                    toast("Failed to go HD");
                                    Log.e(TAG, "Failed to get hd content", e);
                                    // continue
                                }
                            };
                        }.start();
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading image", e);
            }
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
                OutputStream outStream = null;
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                Date date = new Date();
                File file = new File(extStorageDirectory, dateFormat.format(date) + ".PNG");
                File fileDirectory = new File(extStorageDirectory);
                fileDirectory.mkdirs();
                try {
                    outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    Toast.makeText(ImageViewerActivity.this, "Saved", Toast.LENGTH_LONG).show();

                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Toast.makeText(ImageViewerActivity.this, e.toString(), Toast.LENGTH_LONG)
                            .show();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Toast.makeText(ImageViewerActivity.this, e.toString(), Toast.LENGTH_LONG)
                            .show();
                }
                return true;
            }
            case SET_PROFILE: {
                if(mIntent.hasExtra("b64Bytes")) {
                    String b64Bytes = mIntent.getStringExtra("b64Bytes");
                    byte[] data = Base64.decode(b64Bytes);
                    Helpers.updatePicture(ImageViewerActivity.this, data);
                    Toast.makeText(ImageViewerActivity.this,
                            "Set profile picture.", Toast.LENGTH_LONG).show(); 
                    
                } else {
                    Toast.makeText(ImageViewerActivity.this,
                            "Error setting profile picture.", Toast.LENGTH_LONG).show();
                }
                return true;
            }
            default:
                return false;
        }
    }


    public void onPause() {
		super.onPause();
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
        System.gc();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mgr != null) {
            mgr.recycle();
            mgr = null;
        }
    }

    private final void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImageViewerActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
