package edu.stanford.mobisocial.dungbeetle;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentValues;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import android.nfc.*;


public class DungBeetleActivity extends TabActivity
{

	private NfcAdapter mNfcAdapter;
    public static final String TAG = "DungBeetleActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startService(new Intent(this, DungBeetleService.class));

		// Create top-level tabs
		//Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;  
		Intent intent;  

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, ContactsActivity.class);
		spec = tabHost.newTabSpec("contacts").setIndicator(
			"Contacts",
			null).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ObjectsActivity.class);
		spec = tabHost.newTabSpec("objects").setIndicator(
			"Objects",
			null).setContent(intent);
		tabHost.addTab(spec);
		tabHost.setCurrentTab(0);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);


        // DEBUG DATA

        String pubKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDdZjHO9Ef0XS+XqF2lFwxrpnzhNY06TKnrSyjGHbXzxORnHfoLVB0xSCJ6HRI9+/hLWtErTqcmkaJ5YvS074gpfo7kZR5WGapqCe64mTmTCCO8Oxm+PLdIE5w+dYBpCkxMJAdiSscAt6LZHSNYeaxEfBgzmLYTyGYGtC+kNYDSnQIDAQAB";
        ContentValues values = new ContentValues();
        values.put("public_key", pubKeyStr);
        values.put("name", "Aemon Cannon");
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        getContentResolver().insert(url, values);


        // Make every contact a subscriber of the local friend feed
        Cursor contacts = getContentResolver().query(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
                                                     null, null, null, null);
        contacts.moveToFirst();
        while(!contacts.isAfterLast()){
            String id = contacts.getString(contacts.getColumnIndexOrThrow(Contact.PERSON_ID));
            ContentValues vals = new ContentValues();
            vals.put("person_id", id);
            vals.put("feed_name", "friend");
            getContentResolver().insert(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/subscribers"), vals);
            contacts.moveToNext();
        }
	}

	@Override
	protected void onPause() {
        super.onPause();
        if(mNfcAdapter != null){
            mNfcAdapter.disableForegroundNdefPush(this);
        }
	}

	protected void shareContactInfo(){
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {}, 
            "lksdjfdfj".toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfcAdapter.enableForegroundNdefPush(this, ndef);
        Toast.makeText(this, "Touch phones with your friend!", Toast.LENGTH_SHORT).show();
	}

    @Override
	public void onDestroy(){
		super.onDestroy();
	}




}




