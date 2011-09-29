package edu.stanford.mobisocial.dungbeetle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mobisocial.nfc.Nfc;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LinkObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;


public class PickContactsActivity extends TabActivity {

	private ContactListCursorAdapter mContacts;
	private GroupListCursorAdapter mGroups;
    private Intent mIntent;
    private Map<Integer, Contact> mResultContacts = new HashMap<Integer, Contact>();
    private Map<String, Group> mResultGroups = new HashMap<String, Group>();
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
    public static final String INTENT_EXTRA_PARENT_FEED = "feed";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pick_contacts);
		mIntent = getIntent();
		mNfc = new Nfc(this);

		/** Contacts **/
        Cursor c = getContentResolver().query(Uri.parse(
                DungBeetleContentProvider.CONTENT_URI + "/contacts"), null, null, null, Contact.NAME + " COLLATE NOCASE ASC");
		mContacts = new ContactListCursorAdapter(this, c);
		ListView contactsV = (ListView) findViewById(R.id.contacts_list);
        contactsV.setAdapter(mContacts);
        contactsV.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor)mContacts.getItem(position);
                Contact c = new Contact(cursor);
                final CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
                if (checkBox.isChecked()) {
                    checkBox.setChecked(false);
                    mResultContacts.remove(position);
                } else {
                    checkBox.setChecked(true);
                    mResultContacts.put(position, c);
                }
            }
        });

        /** Groups **/
        Cursor d = getContentResolver().query(Uri.parse(
                DungBeetleContentProvider.CONTENT_URI + "/groups"), null, null, null, null);
        mGroups = new GroupListCursorAdapter(this, d);
        ListView groupsV = (ListView) findViewById(R.id.groups_list);
        groupsV.setAdapter(mGroups);
        groupsV.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor)mGroups.getItem(position);
                Group g = new Group(cursor);
                final CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
                if (checkBox.isChecked()) {
                    checkBox.setChecked(false);
                    mResultGroups.remove(g.feedName);
                } else {
                    checkBox.setChecked(true);
                    mResultGroups.put(g.feedName, g);
                }
            }
        });

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec = tabHost.newTabSpec("groups").setIndicator(
            "Groups",null).setContent(R.id.tab2);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Contacts",null).setContent(R.id.tab1);
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


    private void handleOk() {
        Uri data = mIntent.getData();
        String txt = mIntent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d(TAG,"sharing " + mIntent.getType() + ", " + mIntent.getData());
        if (mIntent.getAction().equals(Intent.ACTION_SEND) && mIntent.getType() != null
                && (data != null || txt != null || mIntent.hasExtra(Intent.EXTRA_STREAM))) {
            if (mResultContacts.size() == 0 && mResultGroups.size() == 0) {
                Toast.makeText(this, "No contacts chosen for sharing.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mResultGroups.size() == 0) {
                Toast.makeText(this, "Sending to " + mResultContacts.size() + " contacts...",
                        Toast.LENGTH_SHORT).show();
            } else if (mResultContacts.size() == 0) {
                Toast.makeText(this, "Sending to " + mResultGroups.size() + " feeds...",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sending to " + mResultContacts.size() + " contacts and " +
                        mResultGroups.size() + " groups...", Toast.LENGTH_SHORT).show();
            }

            DbObject outboundObj = null;
            if (mIntent.getType().startsWith("image/") && mIntent.hasExtra(Intent.EXTRA_STREAM)) {
                try {
                    outboundObj = PictureObj.from(this,
                            (Uri)mIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                } catch (IOException e) {
                    Toast.makeText(this, "Error reading photo data.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error reading photo data.", e);
                }
            } else {
                String url = "";
                if (data != null) {
                    url = data.toString();
                } else if (txt != null) {
                    url = txt;
                }
                String title = url;
                if (mIntent.hasExtra(Intent.EXTRA_TITLE)) {
                    title = mIntent.getStringExtra(Intent.EXTRA_TITLE);
                }
                outboundObj = LinkObj.from(url, mIntent.getType(), title);
            }

            if (outboundObj == null) {
                Log.i(TAG, "no content to share.");
                return;
            }
            if (mResultContacts.size() > 0) {
                Helpers.sendMessage(this, mResultContacts.values(), outboundObj);
            }
            if (mResultGroups.size() > 0) {
                for (Group g : mResultGroups.values()) {
                    Helpers.sendToFeed(this, outboundObj, Feed.uriForName(g.feedName));
                }
            }
        } else if (mIntent.getAction().equals(INTENT_ACTION_INVITE) &&
                mIntent.getStringExtra("type").equals("invite_app_feed")) {
            // TODO: Remove?
            Helpers.sendAppFeedInvite(this, mResultContacts.values(),
                    mIntent.getStringExtra("sharedFeedName"),
                    mIntent.getStringExtra("packageName"));
        } else if (mIntent.getAction().equals(INTENT_ACTION_INVITE_TO_THREAD)) {
            Uri threadUri = mIntent.getParcelableExtra("uri");
            Toast.makeText(this, "Sending to " + mResultContacts.size() + " contacts...",
                    Toast.LENGTH_SHORT).show();
            Helpers.sendThreadInvite(this, mResultContacts.values(), threadUri);
        } else if (mIntent.getAction().equals(INTENT_ACTION_PICK_CONTACTS)) {
            long[] ids = new long[mResultContacts.size()];
            Iterator<Contact> it = mResultContacts.values().iterator();
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
            checkBox.setChecked(mResultContacts.containsKey(c.getPosition()));
        }
    }

    private class GroupListCursorAdapter extends CursorAdapter {
        public GroupListCursorAdapter (Context context, Cursor c) {
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
            Group group = new Group(c);
            String name = group.name;
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(name);

            //final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            //icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //((App)getApplication()).contactImages.lazyLoadContactPortrait(contact, icon);

            final CheckBox checkBox = (CheckBox)v.findViewById(R.id.checkbox);
            checkBox.setChecked(mResultGroups.containsKey(group.feedName));
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



