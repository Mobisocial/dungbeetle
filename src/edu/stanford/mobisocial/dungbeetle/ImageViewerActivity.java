package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageViewerActivity extends Activity{
	private BitmapManager mgr = new BitmapManager(1);
	private ImageView im;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		im = (ImageView)findViewById(R.id.image);
		im.setScaleType(ImageView.ScaleType.FIT_CENTER);
		Intent intent = getIntent();

        if(intent.hasExtra("image_url")){
            String url = intent.getStringExtra("image_url");
            ((App)getApplication()).objectImages.lazyLoadImage(
                url.hashCode(), Uri.parse(url), im);
        }
        else if(intent.hasExtra("b64Bytes")){
            String b64Bytes = intent.getStringExtra("b64Bytes");
            ((App)getApplication()).objectImages.lazyLoadImage(
                b64Bytes.hashCode(), b64Bytes, im);
        }


	}


    public void onDestroy() {
		super.onDestroy();
		mgr.recycle();
	}

}



