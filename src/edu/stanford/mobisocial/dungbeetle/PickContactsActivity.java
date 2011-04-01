package edu.stanford.mobisocial.dungbeetle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import java.util.ArrayList;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.app.TabActivity;
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


public class PickContactsActivity extends TabActivity {

    private Intent mIntent;
    private ArrayAdapter<Contact> mResultContacts;
	protected final BitmapManager mgr = new BitmapManager(10);


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pick_contacts);
        mResultContacts = new ContactsAdapter(this);
		mIntent = getIntent();

		ListView resultList = (ListView)findViewById(R.id.result_list);
        resultList.setAdapter(mResultContacts);

        TabHost tabHost = getTabHost();
        Intent intent = new Intent().setClass(this, ContactsActivity.class);
        intent.putExtra("child", true);
        TabHost.TabSpec spec = tabHost.newTabSpec("contacts").setIndicator(
            "Contacts",null).setContent(intent);
        tabHost.addTab(spec);
        intent = new Intent().setClass(this, GroupsActivity.class);
        spec = tabHost.newTabSpec("groups").setIndicator(
            "Groups",null).setContent(intent);
        tabHost.addTab(spec);
        tabHost.setCurrentTab(0);

        registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    Contact contact = (Contact)intent.getSerializableExtra("contact");
                    mResultContacts.add(contact);
                }
            }, new IntentFilter(ContactsActivity.CONTACT_SELECTED));

        Button okButton = (Button)findViewById(R.id.ok_button);
        okButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    handleOk();
                }
            });
    }

	class ContactsAdapter extends ArrayAdapter<Contact> {

        public ContactsAdapter(Context context){
            super(context, R.layout.contacts_item, new ArrayList<Contact>());
        }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)(
                    getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE));
				v = vi.inflate(R.layout.contacts_item, null);
			}

            Contact c = this.getItem(position);

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(c.name);

            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mgr.lazyLoadImage(icon, Gravatar.gravatarUri(c.email));            

            return v;
        }

	}


    private void handleOk(){
        setResult(RESULT_OK, mIntent);
    }

}



