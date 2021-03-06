/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;
import java.util.BitSet;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class HandleNfcContact extends Activity {
    private String mName;
    private String mEmail;
    private static final String TAG = "HandleNfcContact";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		final Uri uri = intent.getData();

		setContentView(R.layout.handle_give);
		Button saveButton = (Button)findViewById(R.id.save_contact_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		Button mutualFriendsButton = (Button)findViewById(R.id.mutual_friends_button);
		mutualFriendsButton.setVisibility(View.GONE);

		if (uri != null && 
		        (uri.getScheme().equals(HomeActivity.SHARE_SCHEME) ||
		         uri.getSchemeSpecificPart().startsWith(FriendRequest.PREFIX_JOIN))){

	        mEmail = uri.getQueryParameter("email");

	        mName = uri.getQueryParameter("name");
            if (mName == null) {
                mName = mEmail;
            
            }

            TextView nameView = (TextView)findViewById(R.id.name_text);
            nameView.setText("Would you like to be friends with " + mName + "?");

            final long cid = FriendRequest.acceptFriendRequest(HandleNfcContact.this, uri, false);
		    saveButton.setOnClickListener(new OnClickListener() {
				    public void onClick(View v) {
                        DBHelper helper = DBHelper.getGlobal(HandleNfcContact.this);
                        IdentityProvider ident = new DBIdentityProvider(helper);

                        try {
                            JSONObject profile = new JSONObject(ident.userProfile());
                            byte[] data = FastBase64.decode(profile.getString("picture"));
                            
                            Helpers.updatePicture(HandleNfcContact.this, data);
                        } catch(Exception e) { }

                        // If asymmetric friend request, send public key.
                        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                            FriendRequest.sendFriendRequest(HandleNfcContact.this, cid, uri.getQueryParameter("cap"));
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
		} else {
            saveButton.setEnabled(false);
            cancelButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
			Toast.makeText(this, "Failed to receive contact.", 
                           Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Failed to handle " + uri);
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



