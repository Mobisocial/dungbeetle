package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageViewerActivity extends Activity{

	private BitmapManager mgr = new BitmapManager(1);
	private ProgressDialog mProgressDialog;
	private ImageView im;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		im = (ImageView)findViewById(R.id.image);
		im.setScaleType(ImageView.ScaleType.FIT_CENTER);
		Intent intent = getIntent();

        if(intent.hasExtra("image_url")){
            String url = intent.getStringExtra("image_url");
            mgr.getBitmap(url.hashCode(), url, new Handler(){
                    public void handleMessage(Message msg){
                        super.handleMessage(msg);
                        Bitmap bm = (Bitmap)msg.obj;
                        if(bm != null){
                            im.setImageBitmap(bm);
                        }
                        if(mProgressDialog != null){
                            mProgressDialog.dismiss();
                        }
                    }
                });
            mProgressDialog = ProgressDialog.show(this,"",
                                                  "Loading...", true);
        }
        else if(intent.hasExtra("b64Bytes")){
            String b64Bytes = intent.getStringExtra("b64Bytes");
            Bitmap bm = mgr.getBitmapB64(b64Bytes.hashCode(), b64Bytes);
            if(bm != null){
                im.setImageBitmap(bm);
            }
        }


	}


    public void onDestroy() {
		super.onDestroy();
		mgr.recycle();
	}

}



