package edu.stanford.mobisocial.dungbeetle;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.database.Cursor;
import android.widget.ListView;
import android.content.Intent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import mobisocial.nfc.Nfc;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.content.Context;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FileObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.widget.TabHost;
import android.app.TabActivity;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.widget.Toast;
import android.widget.CheckBox;


public class PickContactsActivity extends TabActivity {

	private ContactListCursorAdapter mContacts;
    private Intent mIntent;
    private Set<Contact> mResultContacts = new HashSet<Contact>();
	protected final BitmapManager mgr = new BitmapManager(20);
	private Nfc mNfc;

    public static final String TAG = "PickContactsActivity";

    public static final String INTENT_ACTION_INVITE = 
        "edu.stanford.mobisocial.dungbeetle.INVITE";

    public static final String INTENT_ACTION_INVITE_TO_THREAD = 
            "edu.stanford.mobisocial.dungbeetle.INVITE_THREAD";

    public static final String INTENT_ACTION_PICK_CONTACTS = 
        "edu.stanford.mobisocial.dungbeetle.PICK_CONTACTS";

    public static final String INTENT_EXTRA_NFC_SHARE = "mobisocial.dungbeetle.NFC_SHARE";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pick_contacts);
		mIntent = getIntent();
		mNfc = new Nfc(this);

        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            null, null, null, null);
		mContacts = new ContactListCursorAdapter(this, c);

        ListView contactsV = (ListView) findViewById(R.id.contacts_list);
        contactsV.setAdapter(mContacts);
        contactsV.setOnItemClickListener(new OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id){
                    Cursor cursor = (Cursor)mContacts.getItem(position);
                    Contact c = new Contact(cursor);
                    final CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        mResultContacts.remove(c);
                    }
                    else {
                        checkBox.setChecked(true);
                        mResultContacts.add(c);
                    }
                }
            });


        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec = tabHost.newTabSpec("contacts").setIndicator(
            "Contacts",null).setContent(R.id.tab1);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("groups").setIndicator(
            "Groups",null).setContent(R.id.tab2);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);

        Button okButton = (Button)findViewById(R.id.ok_button);
        okButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    handleOk();
                }
            });

        if (getIntent().hasExtra(INTENT_EXTRA_NFC_SHARE)) {
            mNfc.share((NdefMessage)getIntent().getParcelableExtra(INTENT_EXTRA_NFC_SHARE));
        }
    }


    private void handleOk(){
        Uri data = mIntent.getData();
        String txt = mIntent.getStringExtra(Intent.EXTRA_TEXT);
        if(mIntent.getAction().equals(Intent.ACTION_SEND)
           && mIntent.getType() != null
           && (data != null || txt != null)){
            Toast.makeText(this, "Sending to " + mResultContacts.size() + " contacts...",
                    Toast.LENGTH_SHORT).show();
            String url = "";
            if(data != null) url = data.toString();
            else if(txt != null) url = txt;
            Helpers.sendMessage(this, mResultContacts, FileObj.from(mIntent.getType(), url));
        } 
        else if(mIntent.getAction().equals(INTENT_ACTION_INVITE) &&
                mIntent.getStringExtra("type").equals("invite_app_feed")){
            Helpers.sendAppFeedInvite(this, 
                mResultContacts, 
                mIntent.getStringExtra("sharedFeedName"),
                mIntent.getStringExtra("packageName"));
        }
        else if (mIntent.getAction().equals(INTENT_ACTION_INVITE_TO_THREAD)) {
            Uri threadUri = mIntent.getParcelableExtra("uri");
            Toast.makeText(this, "Sending to " + mResultContacts.size() + " contacts...",
                    Toast.LENGTH_SHORT).show();
            Helpers.sendThreadInvite(this, mResultContacts, threadUri);
        }
        else if(mIntent.getAction().equals(INTENT_ACTION_PICK_CONTACTS)){
            long[] ids = new long[mResultContacts.size()];
            Iterator<Contact> it = mResultContacts.iterator();
            int i = 0;
            while(it.hasNext()){
                Contact c = it.next();
                ids[i] = c.id;
                i++;
            }
            mIntent.putExtra("contacts", ids);
        }
        setResult(RESULT_OK, mIntent);
        finish();
    }


    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_picker_item, parent, false);
            bindView(v, context, c);
            return v;
        }

        @Override
        public void bindView(View v, Context context, Cursor c) {
            Contact contact = new Contact(c);
            String name = contact.name;
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(name);

            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ((App)getApplication()).contactImages.lazyLoadContactPortrait(contact, icon);

            final CheckBox checkBox = (CheckBox)v.findViewById(R.id.checkbox);
            checkBox.setChecked(mResultContacts.contains(contact));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfc.onResume(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) return;
    }
}



