/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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
import org.mobisocial.corral.CorralClient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

/**
 * Displays a single image.
 */
public class ImageViewerActivity extends Activity {
    private static final String TAG = "imageViewer";
	private BitmapManager mgr = new BitmapManager(1);
	private ImageView im;

	private Bitmap bitmap;
	private final String extStorageDirectory =
	        Environment.getExternalStorageDirectory().toString() + "/MusubiPictures/";
	private Intent mIntent;
	private CorralClient mCorralClient;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.image_viewer);
        mCorralClient = CorralClient.getInstance(this);
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
                byte[] bytes = FastBase64.decode(content.optString(PictureObj.DATA));
                ((App)getApplication()).objectImages.lazyLoadImage(
                        bytes.hashCode(), bytes, im);
                bitmap = mgr.getBitmap(bytes.hashCode(), bytes);
            } catch (JSONException e) {}
        }
        
        if (mIntent.hasExtra("objHash")) {
            if (!ContentCorral.CONTENT_CORRAL_ENABLED) {
                return;
            }

            long objHash = mIntent.getLongExtra("objHash", -1);
            final DbObj obj = App.instance().getMusubi().objForHash(objHash);
            final JSONObject json = obj.getJson();
            if (json.has(CorralClient.OBJ_LOCAL_URI)) {
                // TODO: this is a proof-of-concept.
                new Thread() {
                    public void run() {
                        try {
                            if (!mCorralClient.fileAvailableLocally(obj)) {
                                //toast("Trying to go HD...");
                            }
                            // Log.d(TAG, "Trying to go HD...");
                            final Uri fileUri = mCorralClient.fetchContent(obj);
                            if (fileUri == null) {
                                try {
                                    Log.d(TAG, "Failed to go HD for " + json.getString(CorralClient.OBJ_LOCAL_URI));
                                } catch (JSONException e) {
                                    Log.d(TAG, "Failed to go HD for " + json);
                                }
                                return;
                            }
                            // Log.d(TAG, "Opening HD file " + fileUri);

                            InputStream is = getContentResolver().openInputStream(fileUri);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 4;

                            Matrix matrix = new Matrix();
                            float rotation = PhotoTaker.rotationForImage(ImageViewerActivity.this, fileUri);
                            if (rotation != 0f) {
                                matrix.preRotate(rotation);
                            }
                            bitmap = BitmapFactory.decodeStream(is, null, options);

                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            bitmap = Bitmap.createBitmap(
                                    bitmap, 0, 0, width, height, matrix, true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    im.setImageBitmap(bitmap);
                                }
                            });
                        } catch (IOException e) {
                            // toast("Failed to go HD");
                            Log.e(TAG, "Failed to get hd content", e);
                            // continue
                        }
                    };
                }.start();
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
                    byte[] data = FastBase64.decode(b64Bytes);
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

    @SuppressWarnings("unused")
    private final void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImageViewerActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
