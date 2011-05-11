package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.net.Uri;
import android.widget.Toast;
import java.util.BitSet;

public class HandleNfcContact extends Activity {
    private String mName;
    private String mEmail;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_give);
		Intent intent = getIntent();
        final Uri uri = intent.getData();
		Button saveButton = (Button)findViewById(R.id.save_contact_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		Button mutualFriendsButton = (Button)findViewById(R.id.mutual_friends_button);
		mutualFriendsButton.setVisibility(View.GONE);

		if(uri != null && uri.getScheme().equals(DungBeetleActivity.SHARE_SCHEME)){
			mName = uri.getQueryParameter("name");
	        mEmail = uri.getQueryParameter("email");

            TextView nameView = (TextView)findViewById(R.id.name_text);
            nameView.setText(mName);

            ImageView portraitView = (ImageView)findViewById(R.id.image);
            if(uri != null){
                ((App)getApplication()).contactImages.lazyLoadContactPortrait(
                    Gravatar.gravatarUri(mEmail, 100), portraitView);
            }
		}
		else{
            saveButton.setVisibility(View.INVISIBLE);
			Toast.makeText(this, "Failed to receive contact :(", 
                           Toast.LENGTH_SHORT).show();
		}

		saveButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    FriendRequest.acceptFriendRequest(HandleNfcContact.this, uri);
                    Toast.makeText(HandleNfcContact.this, "Added " + mName + " as a friend.", Toast.LENGTH_SHORT).show();
                    finish();
				}
			});

		cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});
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



