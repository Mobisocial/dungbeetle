package edu.stanford.mobisocial.dungbeetle.util;
import android.app.ListActivity;
import android.content.Intent;


public class RichListActivity extends ListActivity implements InstrumentedActivity {

    private static int ACTIVITY_CALLOUT = 39;
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
}