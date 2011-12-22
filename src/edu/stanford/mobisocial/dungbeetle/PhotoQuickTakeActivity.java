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

import java.io.IOException;

import mobisocial.socialkit.Obj;

import org.mobisocial.corral.ContentCorral;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

/**
 * Captures an image after long-pressing the 'volume down' key.
 */
public class PhotoQuickTakeActivity extends Activity implements InstrumentedActivity{

    private static int REQUEST_ACTIVITY_CALLOUT = 39;
	private Uri feedUri;
	ActivityCallout mCurrentCallout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_quick_take);
        Intent intent = getIntent();
        if (intent.hasExtra("feed_uri")) {
            feedUri = intent.getParcelableExtra("feed_uri");   
        }
		int orientation = getResources().getConfiguration().orientation;
		setRequestedOrientation(orientation);
        //in case there was an FC, we must restart the service whenever one of our dialogs is opened.
        startService(new Intent(this, DungBeetleService.class));
    }

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
	        event.startTracking();
	        return true;
        }
        return false;
	};
	
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            final Context context = this;
            ((InstrumentedActivity)context).doActivityForResult(new PhotoTaker(
                context, 
                new PhotoTaker.ResultHandler() {
                    @Override
                    public void onResult(Uri imageUri, byte[] data) {
                        Obj obj;
                        Uri storedUri = ContentCorral.storeContent(context, imageUri, "image/jpeg");
                        try {
                            obj = PictureObj.from(context, storedUri);
                        } catch (IOException e) {
                            Log.w("CameraAction", "failed to capture image", e);
                            obj = PictureObj.from(data);
                        }
                        Helpers.sendToFeed(
                            context, obj, feedUri);
                        ((PhotoQuickTakeActivity)context).finish();
                    }
                }, 200, true));
            return true;
        }
        return false;
    }


    @Override
    public void showDialog(Dialog dialog) {
        dialog.show(); // TODO: Figure out how to preserve dialog during screen rotation.
    }

    public void doActivityForResult(ActivityCallout callout) {
        mCurrentCallout = callout;
        Intent launch = callout.getStartIntent();
        startActivityForResult(launch, REQUEST_ACTIVITY_CALLOUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTIVITY_CALLOUT) {
            mCurrentCallout.handleResult(resultCode, data);
        }
        finish();
    }

}
