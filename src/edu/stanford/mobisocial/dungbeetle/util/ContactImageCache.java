package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;

public class ContactImageCache{

	protected final BitmapManager mContactBitmaps;

    public ContactImageCache(int capacity){
        mContactBitmaps = new BitmapManager(capacity, R.drawable.anonymous);
    }

	public void lazyLoadContactPortrait(Contact c, ImageView v){
        lazyLoadContactPortrait(c, v, 80);
	}

	public void lazyLoadContactPortrait(Contact c, ImageView v, int size){
        if(c.picture != null) {
            lazyLoadContactPortrait(c.picture, v);
        }
        else{
            lazyLoadContactPortrait(Gravatar.gravatarUri(c.email, size), v);
        }        
	}

	public void lazyLoadContactPortrait(byte[] bytes, ImageView v){
        mContactBitmaps.lazyLoadImage(v, bytes);
	}

	public void lazyLoadContactPortrait(Uri url, ImageView v){
        mContactBitmaps.lazyLoadImage(v, url);
	}

}