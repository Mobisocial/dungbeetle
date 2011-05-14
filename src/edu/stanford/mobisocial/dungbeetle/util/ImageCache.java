package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;

public class ImageCache {

	protected final BitmapManager mBitmaps;

    public ImageCache(int capacity, int defaultResource){
        mBitmaps = new BitmapManager(capacity, defaultResource);
    }

    public ImageCache(int capacity){
        this(capacity, R.drawable.anonymous);
    }

	public void lazyLoadImage(long id, String b64Bytes, ImageView v){
        mBitmaps.lazyLoadImage(id, v, b64Bytes);
	}

	public void lazyLoadImage(long id, byte[] bytes, ImageView v){
        mBitmaps.lazyLoadImage(id, v, bytes);
	}

	public void lazyLoadImage(long id, Uri url, ImageView v){
        mBitmaps.lazyLoadImage(id, v, url);
	}

    public void invalidate(long id){
        mBitmaps.invalidate(id);
    }

    // TODO: Contact specific helpers -- should factor these into a subclass?
    public void invalidate(Contact c){
        invalidate(c.id);
    }
	public void lazyLoadContactPortrait(Contact c, ImageView v){
        lazyLoadContactPortrait(c, v, 80);
	}
	public void lazyLoadContactPortrait(Contact c, ImageView v, int size){
        if(c.picture != null) {
            lazyLoadImage(c.id, c.picture, v);
        }
        else{
            Uri uri = Gravatar.gravatarUri(c.email, size);
            lazyLoadImage(c.id, uri, v);
        }        
	}



}