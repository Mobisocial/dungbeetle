package edu.stanford.mobisocial.dungbeetle;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import android.nfc.*;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class DungBeetleActivity extends TabActivity
{

	private NfcAdapter mNfcAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startService(new Intent(this, DungBeetleService.class));

		// Create top-level tabs
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;  
		Intent intent;  

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, ContactsActivity.class);
		spec = tabHost.newTabSpec("contacts").setIndicator(
			"Contacts",
			res.getDrawable(R.drawable.icon)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ObjectsActivity.class);
		spec = tabHost.newTabSpec("objects").setIndicator(
			"Objects",
			res.getDrawable(R.drawable.icon)).setContent(intent);
		tabHost.addTab(spec);
		tabHost.setCurrentTab(0);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
	}

	@Override
	protected void onPause() {
        if(mNfcAdapter != null){
            mNfcAdapter.disableForegroundNdefPush(this);
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		menu.clear();
		menu.add(0, 0, 0, "Give contact info to friend.");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case 0: {
			shareContactInfo();
			return true;
		}
		default: return false;
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




