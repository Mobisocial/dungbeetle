package edu.stanford.mobisocial.dungbeetle.util;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;


public class RichActivity extends Activity{

    private static int ACTIVITY_CALLOUT = 39472874;
    private static ActivityCallout mCurrentCallout;
    public static void doActivityForResult(Activity me, ActivityCallout callout) {
    	mCurrentCallout = callout;
    	Intent launch = callout.getStartIntent();
    	me.startActivityForResult(launch, ACTIVITY_CALLOUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_CALLOUT) {
    		mCurrentCallout.handleResult(resultCode, data);
    	}
    }

}