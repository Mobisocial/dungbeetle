package edu.stanford.mobisocial.dungbeetle;
import android.content.ContentValues;
import android.content.Intent;
import edu.stanford.mobisocial.nfc.Nfc;
import java.security.PublicKey;
import android.widget.Toast;
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

    private Nfc mNfc;
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
        mNfc = new Nfc(this);
        mNfc.addNdefHandler(new Nfc.NdefHandler(){
                public int handleNdef(final NdefMessage[] messages){
                    ContactsActivity.this.runOnUiThread(new Runnable(){
                            public void run(){ handleNdef(messages); }
                        });
                    return 1;
                }
            });
	}


    protected void handleNdef(NdefMessage[] messages){
        if(messages.length != 1 || messages[0].getRecords().length != 1){
            Toast.makeText(this, "Oops! expected a single Uri record.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uriStr = new String(messages[0].getRecords()[0].getPayload());
        Uri myURI = Uri.parse(uriStr);
        if(myURI == null || !myURI.getScheme().equals(SHARE_SCHEME)){
            Toast.makeText(this, "Received record without valid Uri!", Toast.LENGTH_SHORT).show();
            return;
        }
        String pubKeyStr = myURI.getQueryParameter("publicKey");
        String name = myURI.getQueryParameter("name");
        String email = myURI.getQueryParameter("email");
        PublicKey pubKey = DBIdentityProvider.publicKeyFromString(pubKeyStr);
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


    public void onItemClick(AdapterView<?> parent, View view, int position, long id){}


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



