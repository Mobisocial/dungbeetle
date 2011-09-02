package edu.stanford.mobisocial.dungbeetle.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;


public class RichActivity extends Activity implements InstrumentedActivity {

    private static int ACTIVITY_CALLOUT = 39472874;
    private static ActivityCallout mCurrentCallout;
    public void doActivityForResult(ActivityCallout callout) {
    	mCurrentCallout = callout;
    	Intent launch = callout.getStartIntent();
    	startActivityForResult(launch, ACTIVITY_CALLOUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_CALLOUT) {
    		mCurrentCallout.handleResult(resultCode, data);
    	}
    }

    @Override
    public void showDialog(Dialog dialog) {
        dialog.show();
    }
}