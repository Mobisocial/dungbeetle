package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import edu.stanford.mobisocial.dungbeetle.util.Base64;
import java.util.*;
import java.net.*;
import java.io.*;
import edu.stanford.mobisocial.dungbeetle.R;


public class BitmapManager{

	private final float hashTableLoadFactor = 0.75f;
	public static final String TAG = "BitmapManager";
	final Map<Long, Bitmap> cache;
	final int cacheSize;
    final int defaultResource;

	public BitmapManager(int cacheSize){
        this(cacheSize, R.drawable.anonymous);
    }

	public BitmapManager(int cacheSize, int defaultResource){
		this.cacheSize = cacheSize;
        this.defaultResource = defaultResource;
		int hashTableCapacity = (int)Math.ceil(cacheSize / hashTableLoadFactor) + 1;
		cache = Collections.synchronizedMap(
			new LinkedHashMap<Long,Bitmap>( 
				hashTableCapacity, hashTableLoadFactor, true) {

				@Override protected boolean removeEldestEntry 
				(Map.Entry<Long,Bitmap> eldest) {
					if(size() > BitmapManager.this.cacheSize){
						Long key = eldest.getKey();
						remove(key);
					}

					// We already handled it manually, so
					// return false.
					return false;
				}

			}); 
	}

	public void getBitmap(final long id, final String url, final Handler handler){
		new Thread(){
			public void run(){
				Bitmap bm = getBitmap(id, url);
				Message m = handler.obtainMessage();
				m.obj = bm;
				handler.sendMessage(m);
			}
		}.start();
	}


	protected boolean hasBitmap(long id){
        return cache.get(id) != null;
    }

	public Bitmap getBitmap(long id, String url){
		Bitmap bm = cache.get(id);
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
					cache.put(id, newBm);
				}
				return newBm;
			}
			catch(IOException e){
				System.err.println();
                return null;
			}
		}
	}

	public Bitmap getBitmap(long id, byte[] bytes){
		Bitmap bm = cache.get(id);
		if(bm != null) {
			return bm;
		}
		else{
            Bitmap newBm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if(newBm != null){
                cache.put(id, newBm);
            }
            return newBm;
		}
	}

	public Bitmap getBitmapB64(long id, String b64Bytes){
		Bitmap bm = cache.get(id);
		if(bm != null) {
			return bm;
		}
		else{
            byte[] bytes = Base64.decode(b64Bytes);
            Bitmap newBm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if(newBm != null){
                cache.put(id, newBm);
            }
            return newBm;
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

	public void lazyLoadImage(final long id, final ImageView im, final Uri uri){
        if(hasBitmap(id)) {
            im.setImageBitmap(getBitmap(id, uri.toString()));
            return;
        }
        im.setImageResource(defaultResource);
		getBitmap(id, uri.toString(), new Handler(){
				public void handleMessage(Message msg){
					super.handleMessage(msg);
					Bitmap bm = (Bitmap)msg.obj;
					if(bm != null){
                        im.setImageBitmap(bm);
					}
				}
			});
	}

	public void lazyLoadImage(final long id, final ImageView im, final byte[] bytes){
        im.setImageBitmap(getBitmap(id, bytes));
	}

	public void lazyLoadImage(final long id, final ImageView im, final String b64Bytes){
        im.setImageBitmap(getBitmapB64(id, b64Bytes));
	}

}