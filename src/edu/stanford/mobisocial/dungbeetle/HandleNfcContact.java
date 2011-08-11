package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.widget.Toast;
import java.util.BitSet;
import org.json.JSONObject;
import edu.stanford.mobisocial.bumblebee.util.Base64;

public class HandleNfcContact extends Activity {
    private String mName;
    private String mEmail;
    private static final String TAG = "HandleNfcContact";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_give);
		Intent intent = getIntent();
        final Uri uri = intent.getData();
		Button saveButton = (Button)findViewById(R.id.save_contact_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		Button mutualFriendsButton = (Button)findViewById(R.id.mutual_friends_button);
		mutualFriendsButton.setVisibility(View.GONE);

		if(uri != null && 
		        (uri.getScheme().equals(DungBeetleActivity.SHARE_SCHEME) ||
		         uri.getSchemeSpecificPart().startsWith(FriendRequest.PREFIX_JOIN))){
			
	        mEmail = uri.getQueryParameter("email");
	        mName = mEmail;

            String mProfile = uri.getQueryParameter("profile");

            //byte[] mPicture = new byte[0];
            
            try{
                JSONObject o = new JSONObject(mProfile);
                mName = o.getString("name");
                //mPicture = Base64.decode(o.getString("picture"));
            }
            catch(Exception e){
            }

            TextView nameView = (TextView)findViewById(R.id.name_text);
            nameView.setText("Would you like to be friends with " + mName + "?");

            final long cid = FriendRequest.acceptFriendRequest(HandleNfcContact.this, uri);
		    saveButton.setOnClickListener(new OnClickListener() {
				    public void onClick(View v) {
                        DBHelper helper = new DBHelper(HandleNfcContact.this);
                        IdentityProvider ident = new DBIdentityProvider(helper);

                        try {
                            JSONObject profile = new JSONObject(ident.userProfile());
                            byte[] data = Base64.decode(profile.getString("picture"));
                            
                            Helpers.updatePicture(HandleNfcContact.this, data);
                        } catch(Exception e) { }

                        // If asymmetric friend request, send public key.
                        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                            FriendRequest.sendFriendRequest(HandleNfcContact.this, cid);
                        }

                        Toast.makeText(HandleNfcContact.this, "Added " + mName + " as a friend.", Toast.LENGTH_SHORT).show();
                        finish();
				    }
			    });

		    cancelButton.setOnClickListener(new OnClickListener() {
				    public void onClick(View v) {
				        Helpers.deleteContact(HandleNfcContact.this, cid);
					    finish();
				    }
			    });

            ImageView portraitView = (ImageView)findViewById(R.id.image);
            if(uri != null){
            /*
                ((App)getApplication()).contactImages.lazyLoadImage(
                    mEmail.hashCode(),
                    Gravatar.gravatarUri(mEmail, 100), 
                    portraitView);
            */
                    
                //((App)getApplication()).contactImages.lazyLoadImage(mPicture.hashCode(), mPicture, portraitView);
            }
		}
		else{
            saveButton.setVisibility(View.INVISIBLE);
			Toast.makeText(this, "Failed to receive contact :(", 
                           Toast.LENGTH_SHORT).show();
		}

	}


    public static BitSet fromByteArray(byte[] bytes) {
        BitSet bits = new BitSet();
        for(int i = 0; i < bytes.length*8; i++) {
            if((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }
}



