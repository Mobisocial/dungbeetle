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

package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.R;

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
}
