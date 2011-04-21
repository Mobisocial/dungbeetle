package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import edu.stanford.mobisocial.dungbeetle.BackupManager.BackupService;
import edu.stanford.mobisocial.dungbeetle.facebook.FacebookInterfaceActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.graphics.BitmapFactory;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import java.util.Collection;
import java.util.Collections;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;


public class ContactsActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private NotificationManager mNotificationManager;
	private static final int REQUEST_SEND_IM = 470;
	private static final int REQUEST_INVITE_TO_GROUP = 471;
	public static final String ACTION_SEND_IM = "mobisocial.db.action.UPDATE_STATUS";
	public static final String TAG = "ContactsActivity";
	private Collection<Contact> mSelection;
	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mHelper = new DBHelper(this);
        Intent intent = getIntent();
        if(intent.hasExtra("group_id")){
            long groupId = intent.getLongExtra("group_id", -1);
            try{
                mGroup = mHelper.groupForGroupId(groupId);
                long gid = mGroup.get().id;
                Cursor c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                              "/group_contacts/" + gid),
                    null,
                    null, null, Contact.NAME + " ASC");
                mContacts = new ContactListCursorAdapter(this, c);
            }
            catch(Maybe.NoValError e){
                Log.i(TAG, "fuck! not found!");
                mContacts = new ContactListCursorAdapter(this, new MatrixCursor(new String[]{}));;
            }
        }
        else{
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
                null, 
                null, null, Contact.NAME + " ASC");
            mContacts = new ContactListCursorAdapter(this, c);
        }

		setListAdapter(mContacts);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setFastScrollEnabled(true);
        registerForContextMenu(lv);
		lv.setOnItemClickListener(this);

		
	}

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = new Contact(cursor);
        menu.setHeaderTitle(c.name);
        
        //String[] menuItems = new String[]{ "Send Message", "Start Application", "Manage Groups", "Delete" };
        String[] menuItems = new String[]{ "Delete" };
        for (int i = 0; i<menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();

        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = new Contact(cursor);

 
        switch(menuItemIndex) {
        case 0:
            Helpers.deleteContact(this, c.id);
            break;
        }
        return true;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor cursor = (Cursor)mContacts.getItem(position);
        final Contact c = new Contact(cursor);


        Intent viewContactIntent = new Intent(ContactsActivity.this, ViewContactTabActivity.class);
        viewContactIntent.putExtra("contact_id", c.id);
        viewContactIntent.putExtra("contact_name", c.name);

        
        Intent intent = getIntent();
        if(intent.hasExtra("group_name")) {
            viewContactIntent.putExtra("group_name", intent.getStringExtra("group_name"));
        }
        
        startActivity(viewContactIntent);

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
        public void bindView(View v, Context context, Cursor cursor) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(name);

            TextView statusText = (TextView) v.findViewById(R.id.status_text);
            statusText.setText(cursor.getString(cursor.getColumnIndexOrThrow(Contact.STATUS)));
            
            int presence = cursor.getInt(cursor.getColumnIndexOrThrow(Contact.PRESENCE));
            
            final Contact c = new Contact(cursor);
           

            //presenceText.setText(Presence.presences[presence]);
            //presenceText.setTextColor(Presence.colors[presence]);

            String email = cursor.getString(cursor.getColumnIndexOrThrow(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            
            if(c.picture != null) {
                icon.setImageBitmap(BitmapFactory.decodeByteArray(c.picture, 0, c.picture.length));
            }

            final ImageView presenceIcon = (ImageView)v.findViewById(R.id.presence_icon);
            switch(presence) {
            case Presence.AVAILABLE:
                presenceIcon.setImageResource(R.drawable.available);
                break;
            case Presence.BUSY:
                presenceIcon.setImageResource(R.drawable.busy);
                break;
            case Presence.AWAY:
                presenceIcon.setImageResource(R.drawable.away);
                break;
            }


            final ImageView more = (ImageView)v.findViewById(R.id.more);

            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ActionItem send_im = new ActionItem();
                        send_im.setTitle("Send IM");
                        //chart.setIcon(getResources().getDrawable(R.drawable.chart));
                        send_im.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mSelection = Collections.singletonList(c);
                                    Intent update = new Intent(ACTION_SEND_IM);
                                    Intent chooser = Intent.createChooser(update, "Send IM");
                                    startActivityForResult(chooser, REQUEST_SEND_IM);
                                }
                            });
                    
                        final ActionItem start_app = new ActionItem();
                        start_app.setTitle("Start App");
                        //production.setIcon(getResources().getDrawable(R.drawable.production));
                        start_app.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.startApplicationWithContact(ContactsActivity.this, Collections.singletonList(c));
                                }
                            });
                    
                        final ActionItem manage_groups = new ActionItem();
                        manage_groups.setTitle("Groups");
                        //production.setIcon(getResources().getDrawable(R.drawable.production));
                        manage_groups.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.showGroupPicker(ContactsActivity.this, c);
                                }
                            });

                    
                        QuickAction qa = new QuickAction(v);

                        qa.addActionItem(send_im);
                        qa.addActionItem(start_app);
                        qa.addActionItem(manage_groups);
                        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

                        qa.show();
                    }
                });
        }

    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    private final static int SHARE_INFO = 0;
    private final static int SET_EMAIL = 1;
    private final static int FACEBOOK_BOOTSTRAP = 2;
    private final static int INVITE_TO_GROUP = 3;
    private final static int LOAD_DB = 4;
    private final static int SAVE_DB = 5;


    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        try{
            Group g = mGroup.get();
            if(g.feedName != null){
                menu.add(0, INVITE_TO_GROUP, 0, "Invite to group");
            }
        } catch(Maybe.NoValError e){
            menu.add(0, LOAD_DB, 0, "Load");
            menu.add(0, SAVE_DB, 0, "Save");
            menu.add(0, SET_EMAIL, 0, "Set email (debug)");
            menu.add(0, FACEBOOK_BOOTSTRAP, 0, "Facebook Bootstrap");
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case SHARE_INFO: {
            ((DungBeetleActivity)getParent()).pushContactInfoViaNfc();
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
		case INVITE_TO_GROUP: {
            Intent i = new Intent();
            i.setAction(PickContactsActivity.INTENT_ACTION_PICK_CONTACTS);
            this.startActivityForResult(i, REQUEST_INVITE_TO_GROUP);
			return true;
        }
		case SAVE_DB: {
			// TODO: should be in DungBeetleActivity.
			doSaveDb();
			return true;
		}
		case LOAD_DB: {
			doLoadDb();
			return true;
		}
        default: return false;
        }
    }

    @Override
    public void finish() {
        super.finish();
        mHelper.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SEND_IM) {
            if (resultCode == RESULT_OK) {
                String im = data.getStringExtra(Intent.EXTRA_TEXT);
                Helpers.sendIM(this, mSelection, im);
            }
        }
        else if (requestCode == REQUEST_INVITE_TO_GROUP) {
            if (resultCode == RESULT_OK) {
                long[] contactIds = data.getLongArrayExtra("contacts");
                try{
                    Helpers.sendGroupInvite(this, contactIds, mGroup.get());
                }catch(Maybe.NoValError e){}
            }
        }
    }
    
    private final void toast(final String text) {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(ContactsActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
    	
    }
    
    private void doLoadDb() {
    	// Choose 
    	BackupService backup = new BackupManager.LocalBackupService();
    	backup.load();
    }
    
    private void doSaveDb() {
    	BackupService backup = new BackupManager.LocalBackupService();
    	backup.store();
    }
}



