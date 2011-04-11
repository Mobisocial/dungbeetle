package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import java.util.*;
import java.net.*;
import java.io.*;
import edu.stanford.mobisocial.dungbeetle.R;


public class BitmapManager{

	private final float hashTableLoadFactor = 0.75f;
	public static final String TAG = "BitmapManager";
	final Map<String, Bitmap> cache;
	final int cacheSize;

	public BitmapManager(int cacheSize){
		this.cacheSize = cacheSize;
		int hashTableCapacity = (int)Math.ceil(cacheSize / hashTableLoadFactor) + 1;
		cache = Collections.synchronizedMap(
			new LinkedHashMap<String,Bitmap>( 
				hashTableCapacity, hashTableLoadFactor, true) {

				@Override protected boolean removeEldestEntry 
				(Map.Entry<String,Bitmap> eldest) {
					if(size() > BitmapManager.this.cacheSize){
						String url = eldest.getKey();
						remove(url);
					}

					// We already handled it manually, so
					// return false.
					return false;
				}

			}); 
	}

	public void getBitmap(final String url, final Handler handler){
		new Thread(){
			public void run(){
				Bitmap bm = getBitmap(url);
				Message m = handler.obtainMessage();
				m.obj = bm;
				handler.sendMessage(m);
			}
		}.start();
	}

	protected boolean hasBitmap(String url){
        return cache.get(url) != null;
    }

	public Bitmap getBitmap(String url){
		Bitmap bm = cache.get(url);
		if(bm != null) {
			return bm;
		}
		else{
			try{
				URL aURL = new URL(url);
				URLConnection conn = aURL.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				Bitmap newBm = BitmapFactory.decodeStream(bis);
				bis.close();
				is.close();
				if(newBm != null){
					cache.put(url, newBm);
				}
				return newBm;
			}
			catch(IOException e){
				System.err.println();
                return null;
			}
		}
	}

	public void recycle(){
		Iterator<Bitmap> it = cache.values().iterator();
		while(it.hasNext()){
			Bitmap bm = it.next();
			bm.recycle();
		}
		cache.clear();
	}

	public void lazyLoadImage(final ImageView im, final Uri uri){
        if(hasBitmap(uri.toString())) {
            im.setImageBitmap(getBitmap(uri.toString()));
            return;
        }
        im.setImageResource(R.drawable.anonymous);
		getBitmap(uri.toString(), new Handler(){
				public void handleMessage(Message msg){
					super.handleMessage(msg);
					Bitmap bm = (Bitmap)msg.obj;
					if(bm != null){
                        im.setImageBitmap(bm);
					}
				}
			});
	}

}