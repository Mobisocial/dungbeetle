package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.facebook.FacebookInterfaceActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ContactsActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mBitmaps = new BitmapManager(10);
	private NotificationManager mNotificationManager;


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent intent = getIntent();
        if(intent.hasExtra("group_id")){
            Long group_id = intent.getLongExtra("group_id", -1);
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_contacts/" + group_id),
                new String[]{Contact._ID, 
                             Contact.NAME, 
                             Contact.EMAIL, 
                             Contact.PERSON_ID,
                             Contact.PUBLIC_KEY},
                null, null, null);
            mContacts = new ContactListCursorAdapter(this, c);
        }
        else{
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
                new String[]{Contact._ID, 
                             Contact.NAME, 
                             Contact.EMAIL, 
                             Contact.PERSON_ID,
                             Contact.PUBLIC_KEY}, 
                null, null, null);
            mContacts = new ContactListCursorAdapter(this, c);
        }

		setListAdapter(mContacts);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
		lv.setOnItemClickListener(this);
	}


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor cursor = (Cursor)mContacts.getItem(position);
        final Contact c = new Contact(cursor);
        final CharSequence[] items = new CharSequence[]{ "Send Message", "Start Application", "Manage Groups" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actions");
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item){
                    case 0:
                        UIHelpers.sendMessageToContact(ContactsActivity.this, 
                                                       Collections.singletonList(c));
                        break;
                    case 1:
                        UIHelpers.startApplicationWithContact(ContactsActivity.this, 
                                                              Collections.singletonList(c));
                        break;
                    case 2:
                    	UIHelpers.showGroupPicker(ContactsActivity.this, c);
                    	break;
                    }
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            bindView(v, context, c);
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndexOrThrow(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(name);

            String email = c.getString(c.getColumnIndexOrThrow(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mBitmaps.lazyLoadImage(icon, Gravatar.gravatarUri(email));
        }

    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    private final static int SHARE_INFO = 0;
    private final static int SET_EMAIL = 1;
    private final static int FACEBOOK_BOOTSTRAP = 2;


    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, SHARE_INFO, 0, "Exchange info");
        menu.add(0, SET_EMAIL, 0, "Set email (debug)");
        menu.add(0, FACEBOOK_BOOTSTRAP, 0, "Facebook Bootstrap");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case SHARE_INFO: {
            ((DungBeetleActivity)getParent()).shareContactInfo();
            return true;
        }
        case SET_EMAIL: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter email:");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DBHelper helper = new DBHelper(ContactsActivity.this);
                        helper.setMyEmail(input.getText().toString());
                    }
                });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();
            return true;
        }
        case FACEBOOK_BOOTSTRAP: {
            Intent intent = new Intent(this, FacebookInterfaceActivity.class);
            startActivity(intent); 
            return true;
        }
        default: return false;
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

}



