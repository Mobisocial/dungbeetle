package edu.stanford.mobisocial.dungbeetle;
import java.util.HashSet;
import java.util.Set;
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
    private Set<Contact> mResultContacts = new HashSet<Contact>();
	protected final BitmapManager mgr = new BitmapManager(10);


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pick_contacts);
        mResultContacts = new HashSet<Contact>();
		mIntent = getIntent();

        TabHost tabHost = getTabHost();
        Intent intent = new Intent().setClass(this, ContactsActivity.class);
        intent.putExtra("showCheckboxes", true);
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
            }, new IntentFilter(ContactsActivity.CONTACT_CHECKED));
        registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    Contact contact = (Contact)intent.getSerializableExtra("contact");
                    mResultContacts.remove(contact);
                }
            }, new IntentFilter(ContactsActivity.CONTACT_UNCHECKED));

        Button okButton = (Button)findViewById(R.id.ok_button);
        okButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    handleOk();
                }
            });
    }


    private void handleOk(){
        setResult(RESULT_OK, mIntent);
    }

}



