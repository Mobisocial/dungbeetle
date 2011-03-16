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
	public final static String LAUNCH_INTENT = 
        "edu.stanford.mobisocial.dungbeetle.HANDLE_NFC_CONTACT";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_give);
		Intent intent = getIntent();
		final String scheme=intent.getScheme();
		if(scheme != null && scheme.equals(ContactsActivity.SHARE_SCHEME)){
			final Uri myURI=intent.getData();
			if(myURI!=null){
				String pubKeyStr = myURI.getQueryParameter("publicKey");
                PublicKey pubKey = DBIdentityProvider.publicKeyFromString(pubKeyStr);
				String name = myURI.getQueryParameter("name");
				String email = myURI.getQueryParameter("email");
                ContentValues values = new ContentValues();
                values.put("public_key", pubKeyStr);
                values.put("name", name);
                values.put("email", email);
                Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
                getContentResolver().insert(url, values);

                values = new ContentValues();
                values.put("person_id", DBIdentityProvider.makePersonIdForPublicKey(pubKey));
                values.put("feed_name", "friend");
                url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/subscribers");
                getContentResolver().insert(url, values);

				Toast.makeText(this, "Received contact info for " + name + ".", 
                               Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, "Received null url...", 
                               Toast.LENGTH_SHORT).show();
			}
		}
		else{
			Toast.makeText(this, "Failed to receive contact :(", 
                           Toast.LENGTH_SHORT).show();
		}


		Button button = (Button)findViewById(R.id.finished_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});

	}

}



