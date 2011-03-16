package edu.stanford.mobisocial.dungbeetle;
import java.security.PublicKey;
import android.widget.Toast;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.view.MenuItem;
import android.view.Menu;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.widget.CursorAdapter;
import android.net.Uri;
import android.database.Cursor;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;


public class ContactsActivity extends ListActivity implements OnItemClickListener{

	private NfcAdapter mNfcAdapter;
	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            new String[]{Contact._ID, Contact.NAME, Contact.PUBLIC_KEY}, 
            null, null, null);
		mContacts = new ContactListCursorAdapter(this, c);
		setListAdapter(mContacts);
		getListView().setOnItemClickListener(this);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
		// JSONObject o = mContacts.getItem(position);
		// String userId = o.optString("id");
		// Intent intent = new Intent(ViewProfileActivity.LAUNCH_INTENT);
		// intent.putExtra("user_id", userId);
		// startActivity(intent);
	}


	private final static int SHARE_CONTACT = 0;

	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		menu.clear();
		menu.add(0, 0, 0, "Share Contact Info");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case SHARE_CONTACT: {
			shareContactInfo();
			return true;
		}
		default: return false;
		}
	}

	protected void shareContactInfo(){
        DBHelper helper = new DBHelper(this);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String name = ident.userName();
        String email = ident.userEmail();
        PublicKey pubKey = ident.userPublicKey();
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SHARE_SCHEME);
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
        mNfcAdapter.enableForegroundNdefPush(this, ndef);
        Toast.makeText(this, "Touch phones with your friend!", Toast.LENGTH_SHORT).show();
        helper.close();
	}

	@Override
	public void onPause() {
        super.onPause();
        if(mNfcAdapter != null){
            mNfcAdapter.disableForegroundNdefPush(this);
        }
	}

    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            String name = c.getString(c.getColumnIndex(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndex(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }
        }

    }


}



