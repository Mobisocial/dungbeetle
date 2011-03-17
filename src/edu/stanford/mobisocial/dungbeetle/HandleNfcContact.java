package edu.stanford.mobisocial.dungbeetle;
import java.security.PublicKey;
import android.content.ContentValues;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.net.Uri;
import android.widget.Toast;


public class HandleNfcContact extends Activity {
    
    private String mName;
    private String mEmail;
    private String mPubKeyStr;
    private PublicKey mPubKey;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_give);
		Intent intent = getIntent();
        Uri uri = intent.getData();
		Button saveButton = (Button)findViewById(R.id.save_contact_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_button);

		if(uri != null && uri.getScheme().equals(ContactsActivity.SHARE_SCHEME)){
            mName = uri.getQueryParameter("name");
            mEmail = uri.getQueryParameter("email");
            mPubKeyStr = uri.getQueryParameter("publicKey");
            mPubKey = DBIdentityProvider.publicKeyFromString(mPubKeyStr);
		}
		else{
            saveButton.setVisibility(View.INVISIBLE);
			Toast.makeText(this, "Failed to receive contact :(", 
                           Toast.LENGTH_SHORT).show();
		}

		saveButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    ContentValues values = new ContentValues();
                    values.put("public_key", mPubKeyStr);
                    values.put("name", mName);
                    values.put("email", mEmail);
                    Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
                    getContentResolver().insert(url, values);

                    values = new ContentValues();
                    values.put("person_id", DBIdentityProvider.makePersonIdForPublicKey(mPubKey));
                    values.put("feed_name", "friend");
                    url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/subscribers");
                    getContentResolver().insert(url, values);
				}
			});

		cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});
	}

}



