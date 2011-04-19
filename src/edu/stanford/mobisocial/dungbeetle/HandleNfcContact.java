package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.widget.ImageView;
import android.widget.TextView;
import java.security.PublicKey;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.net.Uri;
import android.widget.Toast;
import java.util.BitSet;
import com.skjegstad.utils.BloomFilter;
import android.util.Base64;
import android.widget.ListAdapter;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.DialogInterface;


public class HandleNfcContact extends Activity {
    private String mName;
    private String mEmail;
    private String mPubKeyStr;
    private PublicKey mPubKey;
    private BitSet mFilterData;
    private int mBitSetSize;
    private int mExpectedNumberOElements;
    private int mActualNumberOfFilterElements;
	private BitmapManager mgr = new BitmapManager(1);
	protected final BitmapManager mBitmaps = new BitmapManager(20);
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_give);
		Intent intent = getIntent();
        Uri uri = intent.getData();
		Button saveButton = (Button)findViewById(R.id.save_contact_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		Button mutualFriendsButton = (Button)findViewById(R.id.mutual_friends_button);

		if(uri != null && uri.getScheme().equals(DungBeetleActivity.SHARE_SCHEME)){
            mName = uri.getQueryParameter("name");
            mEmail = uri.getQueryParameter("email");
            mPubKeyStr = uri.getQueryParameter("publicKey");
            mPubKey = DBIdentityProvider.publicKeyFromString(mPubKeyStr);

            mFilterData = fromByteArray(Base64.decode(uri.getQueryParameter("filterData"), Base64.DEFAULT));
            mBitSetSize = Integer.parseInt(uri.getQueryParameter("bitSetSize"));
            mExpectedNumberOElements = Integer.parseInt(uri.getQueryParameter("expectedNumberOfFilterElements"));
            mActualNumberOfFilterElements = Integer.parseInt(uri.getQueryParameter("actualNumberOfFilterElements"));

            BloomFilter friendsFilter = new BloomFilter(mBitSetSize, mExpectedNumberOElements, mActualNumberOfFilterElements, mFilterData);
            final Contact[] friends = Helpers.checkFriends(this, friendsFilter);


            final ListAdapter adapter = new ArrayAdapter<Contact>(getApplicationContext(), R.layout.mutual_friend_row, friends) {

                ViewHolder holder;
                Drawable icon;

                class ViewHolder {
                    ImageView icon;
                    TextView title;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    final LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

                    if (convertView == null) {
                        convertView = inflater.inflate(
                        R.layout.mutual_friend_row, null);

                        holder = new ViewHolder();
                        holder.icon = (ImageView) convertView
                        .findViewById(R.id.icon);
                        holder.title = (TextView) convertView
                        .findViewById(R.id.title);
                        convertView.setTag(holder);
                    } 
                    else {
                        // view already defined, retrieve view holder
                        holder = (ViewHolder) convertView.getTag();
                    }		

                    //tile = getResources().getDrawable(R.drawable.list_icon); //this is an image from the drawables folder

                    holder.title.setText(friends[position].name);
                    //holder.icon.setImageDrawable(tile);
                    String email = friends[position].email;
                    holder.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mBitmaps.lazyLoadImage(holder.icon, Gravatar.gravatarUri(email));
                    
                    return convertView;
                }
            };

            if(friends.length == 0) {
                mutualFriendsButton.setText("No mutual friends");
            }
            else {
                if(friends.length == 1) {
                    mutualFriendsButton.setText("1 mutual friend");
                }
                else {
                    mutualFriendsButton.setText(friends.length + " mutual friends");
                }
                mutualFriendsButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        
                        AlertDialog.Builder builder = new AlertDialog.Builder(HandleNfcContact.this);
                        builder.setTitle("Mutual Friends");
                        builder.setAdapter(adapter,new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                //Toast.makeText(HandleNfcContact.this, "You selected: " + friends[item].name,Toast.LENGTH_LONG).show();
                                //dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
            }


            TextView nameView = (TextView)findViewById(R.id.name_text);
            nameView.setText(mName);

            ImageView portraitView = (ImageView)findViewById(R.id.image);
            //portraitView.setImageResource(R.drawable.ellipsis);
            if(uri != null){
                mgr.lazyLoadImage(portraitView, Gravatar.gravatarUri(mEmail, 100));
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



