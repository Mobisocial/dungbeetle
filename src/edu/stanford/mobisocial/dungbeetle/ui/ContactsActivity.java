package edu.stanford.mobisocial.dungbeetle.ui;
import java.util.Collections;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.AboutActivity;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleService;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.SearchActivity;
import edu.stanford.mobisocial.dungbeetle.UIHelpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

/**
 * Displays a list of contacts. If the intent used to create
 * this activity as Long extra "group_id", contacts are chosen
 * from this group. Otherwise, lists all known contacts.
 *
 */
public class ContactsActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private static final int REQUEST_INVITE_TO_GROUP = 471;
	public static final String TAG = "ContactsActivity";

	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();

    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
        if(Build.VERSION.SDK_INT < 11)
        	intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	else 
    		intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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

    public void onClickNew(View v) {
        Intent share = new Intent(Intent.ACTION_SEND);
        Uri friendRequest = FriendRequest.getInvitationUri(this);
        share.putExtra(Intent.EXTRA_TEXT,
                "Be my friend on Musubi! Click here from your Android device: "
                + friendRequest);
        share.putExtra(Intent.EXTRA_SUBJECT, "Join me on Musubi!");
        share.setType("text/plain");
        startActivity(share);
    }

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mHelper = DBHelper.getGlobal(this);
        Intent intent = getIntent();

        if (intent.hasExtra("group_id")) {    
    		setContentView(R.layout.group_contacts);
            long groupId = intent.getLongExtra("group_id", -1);
            try {
                mGroup = mHelper.groupForGroupId(groupId);
                long gid = mGroup.get().id;
                Cursor c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_contacts/" + gid),
                    null, Contact.HIDDEN + "=0", null, Contact.NAME + " COLLATE NOCASE ASC");
                mContacts = new ContactListCursorAdapter(this, c);
            } catch(Maybe.NoValError e) {
                Log.i(TAG, "group not found!");
                mContacts = new ContactListCursorAdapter(this, new MatrixCursor(new String[]{}));
            }
        } else {
        	setContentView(R.layout.contacts);
            setTitleFromActivityLabel (R.id.title_text);
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
                null, 
                Contact.HIDDEN + "=0", null, Contact.NAME + " COLLATE NOCASE ASC");
            mContacts = new ContactListCursorAdapter(this, c);
        }

		setListAdapter(mContacts);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setFastScrollEnabled(true);
        registerForContextMenu(lv);
		lv.setOnItemClickListener(this);
		//lv.setCacheColorHint(Feed.colorFor(groupName, Feed.BACKGROUND_ALPHA));
		
		//THIS is not derived from musubibaseactivity so it needs its own
        startService(new Intent(this, DungBeetleService.class));
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = Helpers.getContact(v.getContext(), cursor.getLong(cursor.getColumnIndexOrThrow(Contact._ID)));
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
        final Contact c = Helpers.getContact(this, cursor.getLong(cursor.getColumnIndexOrThrow(Contact._ID)));

 
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
        Helpers.getContact(view.getContext(), cursor.getLong(cursor.getColumnIndexOrThrow(Contact._ID))).view(this);
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
            final Contact c = Helpers.getContact(context, cursor.getLong(cursor.getColumnIndexOrThrow(Contact._ID)));

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(c.name);

            TextView statusText = (TextView) v.findViewById(R.id.status_text);
            statusText.setText(c.status);
            
            TextView unreadCount = (TextView)v.findViewById(R.id.unread_count);
            unreadCount.setTextColor(Color.RED);
            unreadCount.setText(c.numUnread + " unread");
            unreadCount.setVisibility(c.numUnread == 0 ? View.INVISIBLE : View.VISIBLE);
            
            
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setImageBitmap(c.picture);
            
            final ImageView presenceIcon = (ImageView)v.findViewById(R.id.presence_icon);
            presenceIcon.setImageResource(c.currentPresenceResource());

            final ImageView nearbyIcon = (ImageView)v.findViewById(R.id.nearby_icon);
        	nearbyIcon.setVisibility(c.nearby ? View.VISIBLE : View.GONE);
        	
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
                        /*
                        final ActionItem start_app = new ActionItem();
                        start_app.setTitle("Start App");
                        start_app.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AppReferenceObj.promptForApplication(ContactsActivity.this, new AppReferenceObj.Callback() {
                                        @Override
                                        public void onAppSelected(String packageName, String arg, Intent localLaunch) {
                                            DbObject obj = new AppReference(packageName, arg);
                                            Helpers.sendMessage(ContactsActivity.this, Collections.singletonList(c), obj);
                                            startActivity(localLaunch);
                                        }
                                    });
                                }
                            });
                    */
                        final ActionItem manage_groups = new ActionItem();
                        manage_groups.setTitle("Show Groups");
                        manage_groups.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.showGroupPicker(
                                        ContactsActivity.this, 
                                        c,
                                        null);
                                }
                            });

                        QuickAction qa = new QuickAction(v);
                        //qa.addActionItem(send_im);
                        //qa.addActionItem(start_app);
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
                try {
                    Helpers.sendGroupInvite(this, contactIds, mGroup.get());
                } catch(Maybe.NoValError e) {}
            }
        }
    }

    @SuppressWarnings("unused")
    private final void toast(final String text) {
    	runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
