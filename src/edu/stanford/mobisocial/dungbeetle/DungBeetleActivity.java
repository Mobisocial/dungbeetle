package edu.stanford.mobisocial.dungbeetle;
import android.nfc.NdefMessage;
import android.widget.Toast;
import edu.stanford.mobisocial.nfc.Nfc;
import android.nfc.NdefRecord;
import android.net.Uri;
import java.security.PublicKey;
import android.view.View;
import android.widget.Button;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.view.View.OnClickListener;

public class DungBeetleActivity extends TabActivity
{

    public static final String TAG = "DungBeetleActivity";
    private Nfc mNfc;

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
			"Feed",
			null).setContent(intent);
		tabHost.addTab(spec);
		
		// Do the same for the other tabs
        intent = new Intent().setClass(this, ProfileActivity.class);
        spec = tabHost.newTabSpec("profile").setIndicator("Profile",
                          null)
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, FriendsActivity.class);
        spec = tabHost.newTabSpec("friends").setIndicator("Friends",
                          null)
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, GroupsActivity.class);
        spec = tabHost.newTabSpec("groups").setIndicator("Groups",
                          null)
                      .setContent(intent);
        tabHost.addTab(spec);
        
		tabHost.setCurrentTab(0);


		Button button = (Button)findViewById(R.id.share_info_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    shareContactInfo();
				}
			});

        mNfc = new Nfc(this);
        mNfc.addNdefHandler(new Nfc.NdefHandler(){
                public int handleNdef(final NdefMessage[] messages){
                    DungBeetleActivity.this.runOnUiThread(new Runnable(){
                            public void run(){
                                doHandleNdef(messages);
                            }
                        });
                    return NDEF_CONSUME;
                }
            });


	}

    protected void doHandleNdef(NdefMessage[] messages){
        if(messages.length != 1 || messages[0].getRecords().length != 1){
            Toast.makeText(this, "Oops! expected a single Uri record. ",
                           Toast.LENGTH_SHORT).show();
            return;
        }
        String uriStr = new String(messages[0].getRecords()[0].getPayload());
        Uri myUri = Uri.parse(uriStr);
        if(myUri == null || !myUri.getScheme().equals(ContactsActivity.SHARE_SCHEME)){
            Toast.makeText(this, "Received record without valid Uri!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent().setClass(this, HandleNfcContact.class);
        intent.setData(myUri);
        startActivity(intent);
    }


    protected void shareContactInfo(){
        DBHelper helper = new DBHelper(this);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String name = ident.userName();
        String email = ident.userEmail();
        PublicKey pubKey = ident.userPublicKey();
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContactsActivity.SHARE_SCHEME);
        builder.authority("dungbeetle");
        builder.appendQueryParameter("name", name);
        builder.appendQueryParameter("email", email);
        builder.appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey));
        Uri uri = builder.build();
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {},
            uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.share(ndef);
        Toast.makeText(this, "Touch phones with your friend!", Toast.LENGTH_SHORT).show();
        helper.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfc.onResume(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) {
            return;
        }
    }

    @Override
	public void onDestroy(){
		super.onDestroy();
	}


}




