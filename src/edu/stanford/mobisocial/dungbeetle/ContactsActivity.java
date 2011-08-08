package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
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
import edu.stanford.mobisocial.dungbeetle.facebook.FacebookInterfaceActivity;
import edu.stanford.mobisocial.dungbeetle.google.OAuthFlowApp;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.objects.ActivityPullObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import java.util.Collections;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;


public class ContactsActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private static final int REQUEST_INVITE_TO_GROUP = 471;
	public static final String TAG = "ContactsActivity";

	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();

/*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, DungBeetleActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (getTitle ());
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }


    public void onClickSearch (View v)
    {
        startActivity (new Intent(getApplicationContext(), SearchActivity.class));
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mHelper = new DBHelper(this);
        Intent intent = getIntent();
        String groupName = "";

        if(intent.hasExtra("group_id")){    
        		setContentView(R.layout.group_contacts);
            long groupId = intent.getLongExtra("group_id", -1);
            try{
                mGroup = mHelper.groupForGroupId(groupId);
                groupName = mGroup.get().name;
                long gid = mGroup.get().id;
                Cursor c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                              "/group_contacts/" + gid),
                    null,
                    null, null, Contact.NAME + " ASC");
                mContacts = new ContactListCursorAdapter(this, c);
            }
            catch(Maybe.NoValError e){
                Log.i(TAG, "group not found!");
                mContacts = new ContactListCursorAdapter(this, new MatrixCursor(new String[]{}));
            }
        }
        else{
        		setContentView(R.layout.contacts);
            setTitleFromActivityLabel (R.id.title_text);
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
        //registerForContextMenu(lv);
		lv.setOnItemClickListener(this);
		//lv.setCacheColorHint(Feed.colorFor(groupName, Feed.BACKGROUND_ALPHA));
		
	}

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = new Contact(cursor);
        menu.setHeaderTitle(c.name);
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

    public void onClickNew(View v) {
        Intent share = new Intent(Intent.ACTION_SEND);
        Uri friendRequest = FriendRequest.getInvitationUri(this);
        share.putExtra(Intent.EXTRA_TEXT,
                "Be my friend on DungBeetle! Click here from your Android device: "
                + friendRequest);
        share.putExtra(Intent.EXTRA_SUBJECT, "Join me on DungBeetle!");
        share.setType("text/plain");
        startActivity(share);
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
            final Contact c = new Contact(cursor);

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(c.name);

            TextView statusText = (TextView) v.findViewById(R.id.status_text);
            statusText.setText(c.status);
            
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            ((App)getApplication()).contactImages.lazyLoadContactPortrait(c, icon);

            final ImageView presenceIcon = (ImageView)v.findViewById(R.id.presence_icon);
            presenceIcon.setImageResource(c.currentPresenceResource());

            final ImageView more = (ImageView)v.findViewById(R.id.more);

            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ActionItem send_im = new ActionItem();
                        send_im.setTitle("Send IM");
                        send_im.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.sendIM(ContactsActivity.this, 
                                                     Collections.singletonList(c));
                                }
                            });
                    
                        final ActionItem start_app = new ActionItem();
                        start_app.setTitle("Start App");
                        start_app.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AppReferenceObj.promptForApplication(ContactsActivity.this, new AppReferenceObj.Callback() {
                                        @Override
                                        public void onAppSelected(String packageName, String arg, Intent localLaunch) {
                                            /*Helpers.sendMessage(ContactsActivity.this, Collections.singletonList(c),
                                                    new DbObject(InviteToSharedAppFeedObj.TYPE,
                                                            AppReferenceObj.json(packageName, arg)));
                                            ContactsActivity.this.startActivity(localLaunch);*/

                                            
                                            DbObject obj = new AppReference(packageName, arg);
                                            Helpers.sendMessage(ContactsActivity.this, Collections.singletonList(c), obj);
                                            startActivity(localLaunch);
                                        }
                                    });
                                }
                            });
                    
                        final ActionItem manage_groups = new ActionItem();
                        manage_groups.setTitle("Show Groups");
                        manage_groups.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.showGroupPicker(
                                        ContactsActivity.this, 
                                        c);
                                }
                            });

                        final ActionItem join_activity = new ActionItem();
                        join_activity.setTitle("Join Activity");
                        join_activity.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityPullObj.activityForContact(ContactsActivity.this, c);
                                }
                            });
                    
                        QuickAction qa = new QuickAction(v);

                        //qa.addActionItem(send_im);
                        qa.addActionItem(start_app);
                        qa.addActionItem(manage_groups);
                        //qa.addActionItem(join_activity);
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
    private final static int INVITE_EMAIL = 6;
    private final static int GOOGLE_BOOTSTRAP = 7;


    /*public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        if(mGroup.isKnown()){
            menu.add(0, INVITE_TO_GROUP, 0, "Invite to group");
        }
        else{
        	menu.add(0, INVITE_EMAIL, 0, "Invite over email");
            //menu.add(0, LOAD_DB, 0, "Load");
            //menu.add(0, SAVE_DB, 0, "Save");
            menu.add(0, SET_EMAIL, 0, "Set email (debug)");
            menu.add(0, FACEBOOK_BOOTSTRAP, 0, "Facebook Bootstrap");
            menu.add(0, GOOGLE_BOOTSTRAP, 0, "Google Bootstrap");
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
        case GOOGLE_BOOTSTRAP: {
            Intent intent = new Intent(this, OAuthFlowApp.class);
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
		case INVITE_EMAIL: {
			doSendInviteEmail();
			return true;
		}
        default: return false;
        }
    }*/

    @Override
    public void finish() {
        super.finish();
        mHelper.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INVITE_TO_GROUP) {
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

    private void doSendInviteEmail() {
    	String subject = "Meet me on DungBeetle!";
    	StringBuilder email = new StringBuilder();
    	Uri inviteUri = WebContentHandler.getWebFriendlyUri(FriendRequest.getInvitationUri(this));
    	Uri downloadUri = Uri.parse(DungBeetleActivity.AUTO_UPDATE_URL_BASE
                                    + "/" + DungBeetleActivity.AUTO_UPDATE_APK_FILE);
    	email.append("Meet me on DungBeetle for Android!<br/><br/>");
    	email.append("If you don't already have DungBeetle, get it ");
    	email.append("<a href=\"" + downloadUri + "\">here.</a><br/><br/>");
    	email.append("Then, click ");
    	email.append("<a href=\"" + inviteUri + "\">here</a>");
    	email.append(" to connect with me.");

    	Intent send = new Intent(Intent.ACTION_SEND);
    	send.setType("text/html");
    	send.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email.toString()));
    	send.putExtra(Intent.EXTRA_SUBJECT, subject);
    	
    	if (getPackageManager().queryIntentActivities(send, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
    		toast("No way to send an invitation");
    	} else {
    		startActivity(send);
    	}
    }
}


