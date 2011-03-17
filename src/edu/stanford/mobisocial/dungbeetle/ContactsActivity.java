package edu.stanford.mobisocial.dungbeetle;
import android.os.Message;
import android.os.Handler;
import android.widget.Button;
import android.content.Intent;
import edu.stanford.mobisocial.nfc.Nfc;
import java.security.PublicKey;
import android.widget.Toast;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
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
import android.view.View.OnClickListener;

public class ContactsActivity extends ListActivity implements OnItemClickListener{

    private Nfc mNfc;
	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";

	private Handler mNfcHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                doHandleNdef((NdefMessage[])msg.obj);
            }
        };


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
        mNfc = new Nfc(this);
        mNfc.addNdefHandler(new Nfc.NdefHandler(){
                public int handleNdef(final NdefMessage[] messages){
                    Message m = mNfcHandler.obtainMessage();
                    m.obj = messages;
                    mNfcHandler.sendMessage(m);
                    return NDEF_CONSUME;
                }
            });

		Button button = (Button)findViewById(R.id.share_info_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    shareContactInfo();
				}
			});
	}


    protected void doHandleNdef(NdefMessage[] messages){
        if(messages.length != 1 || messages[0].getRecords().length != 1){
            Toast.makeText(this, "Oops! expected a single Uri record.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uriStr = new String(messages[0].getRecords()[0].getPayload());
        Uri myUri = Uri.parse(uriStr);
        if(myUri == null || !myUri.getScheme().equals(SHARE_SCHEME)){
            Toast.makeText(this, "Received record without valid Uri!", Toast.LENGTH_SHORT).show();
            return;
        }
		Intent intent = new Intent().setClass(this, HandleNfcContact.class);
		intent.setData(myUri);
		startActivity(intent);
    }


    public void onItemClick(AdapterView<?> parent, View view, int position, long id){}

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



