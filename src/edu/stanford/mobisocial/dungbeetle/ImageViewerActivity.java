package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ImageViewerActivity extends Activity{
	private BitmapManager mgr = new BitmapManager(1);
	private ImageView im;

	private Bitmap bitmap;
	private String extStorageDirectory;
	private Intent mIntent;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		im = (ImageView)findViewById(R.id.image);
		im.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mIntent = getIntent();

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
        }

        extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/DungBeetlePictures/";


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


    public void onDestroy() {
		super.onDestroy();
		mgr.recycle();
	}

}
