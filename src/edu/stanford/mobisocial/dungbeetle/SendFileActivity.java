package edu.stanford.mobisocial.dungbeetle;

import android.content.Intent;

import mobisocial.nfc.Nfc;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FileObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import android.app.Activity;
import edu.stanford.mobisocial.dungbeetle.util.AndroidActivityHelpers;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.os.Bundle;
import android.net.Uri;
import android.widget.Toast;


public class SendFileActivity extends Activity {
    private static final int REQUEST_FEED = 0;
	protected final BitmapManager mgr = new BitmapManager(20);
	private Nfc mNfc;

    public static final String TAG = "PickContactsActivity";

    public static final String INTENT_ACTION_INVITE = 
        "edu.stanford.mobisocial.dungbeetle.INVITE";

    public static final String INTENT_ACTION_PICK_CONTACTS = 
        "edu.stanford.mobisocial.dungbeetle.PICK_CONTACTS";

    public static final String INTENT_EXTRA_NFC_SHARE = "mobisocial.dungbeetle.NFC_SHARE";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	

    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   if (requestCode == REQUEST_FEED && resultCode == Activity.RESULT_OK) {
	       handleOk(getIntent());
	   } else {
	       AndroidActivityHelpers.setContext(this);
	       AndroidActivityHelpers.toast("not doing anything.");
	       finish();
	   }
	}

    private void handleOk(Intent intent){
        Uri feed = intent.getParcelableExtra(DbObject.EXTRA_FEED_URI);
        Uri data = intent.getData();
        String txt = intent.getStringExtra(Intent.EXTRA_TEXT);
        if(intent.getAction().equals(Intent.ACTION_SEND) && intent.getType() != null
                && (data != null || txt != null)) {
            Toast.makeText(this, "Sending to feed...", Toast.LENGTH_SHORT).show();
            String url = "";
            if(data != null) url = data.toString();
            else if(txt != null) url = txt;
            Helpers.sendToFeed(this, FileObj.from(intent.getType(), url), feed);
        }
    }
}



