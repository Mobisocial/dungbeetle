package edu.stanford.mobisocial.dungbeetle;


import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import android.app.Dialog;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;

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
                    public void onResult(byte[] data) {
                        DbObject obj = PictureObj.from(data);
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
