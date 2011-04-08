package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import android.widget.ImageView;
import android.widget.TextView;
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
	private BitmapManager mgr = new BitmapManager(1);
    
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

            TextView nameView = (TextView)findViewById(R.id.name_text);
            nameView.setText(mName);

            ImageView portraitView = (ImageView)findViewById(R.id.image);
            //portraitView.setImageResource(R.drawable.ellipsis);
            if(uri != null){
                mgr.lazyLoadImage(portraitView, Gravatar.gravatarUri(mEmail));
            }
		}
		else{
            saveButton.setVisibility(View.INVISIBLE);
			Toast.makeText(this, "Failed to receive contact :(", 
                           Toast.LENGTH_SHORT).show();
		}

		saveButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    Uri uri = Helpers.insertContact(HandleNfcContact.this, 
                                          mPubKeyStr, mName, mEmail);
                    long contactId = Long.valueOf(uri.getLastPathSegment());
                    Helpers.insertSubscriber(HandleNfcContact.this,
                        contactId,
                        "friend");
                    finish();
				}
			});

		cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});
	}

}



