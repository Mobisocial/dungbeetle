package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
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
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppObj;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import java.util.Collection;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import java.util.UUID;

import mobisocial.nfc.NdefFactory;





public class GroupsActivity extends ListActivity implements OnItemClickListener{
	private GroupListCursorAdapter mGroups;
    public static final String SHARE_SCHEME = "db-share-contact";
	private static final int REQUEST_INVITE_TO_GROUP = 1;
	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groups);
        mHelper = new DBHelper(this);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"),
            null, null, null, Group.NAME + " ASC");
		mGroups = new GroupListCursorAdapter(this, c);
		setListAdapter(mGroups);
		getListView().setOnItemClickListener(this);
		registerForContextMenu(getListView());
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor)mGroups.getItem(info.position);
        final Group g = new Group(cursor);
        menu.setHeaderTitle(g.name);
        String[] menuItems = new String[]{ "Delete", "Write to Tag" };
        for (int i = 0; i< menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        Cursor cursor = (Cursor)mGroups.getItem(info.position);
        final Group g = new Group(cursor);
        switch(menuItemIndex) {
        case 0:
            Helpers.deleteGroup(GroupsActivity.this, g.id);
            break;
        case 1:
            if(g.dynUpdateUri != null){
                Uri uri = Uri.parse(g.dynUpdateUri);
                ((DungBeetleActivity)getParent()).writeGroupToTag(uri);
            }
            else{
				Toast.makeText(this, "Invalid group.", Toast.LENGTH_SHORT).show();
            }
            break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
        Cursor cursor = (Cursor)mGroups.getItem(position);
        final Group g = new Group(cursor);
        Intent viewGroupIntent = new Intent(GroupsActivity.this, GroupsTabActivity.class);
        viewGroupIntent.putExtra("group_id", g.id);
        viewGroupIntent.putExtra("group_name", g.name);
        viewGroupIntent.putExtra("group_uri", g.dynUpdateUri);
        startActivity(viewGroupIntent);
    }

    private class GroupListCursorAdapter extends CursorAdapter {
        public GroupListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }
        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.groups_item, parent, false);
            bindView(v, context, c);
            return v;
        }
        @Override
        public void bindView(View v, Context context, Cursor c) {

            final Group g = new Group(c);
            final Collection<Contact> contactsInGroup = g.contactCollection(mHelper);
        
            String name = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }           

            ImageView more = (ImageView) v.findViewById(R.id.more);

            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mGroup = Maybe.definitely(g);
                    
                        final ActionItem sendIM = new ActionItem();
                        sendIM.setTitle("Send IM");
                        sendIM.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.sendIM(
                                        GroupsActivity.this, 
                                        contactsInGroup);
                                }
                            });
                    
                        final ActionItem startApp = new ActionItem();
                        startApp.setTitle("Start App");
                        startApp.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    InviteToSharedAppObj.promptForApplication(GroupsActivity.this, new InviteToSharedAppObj.Callback() {
                                        @Override
                                        public void onAppSelected(String packageName, String arg, Intent localLaunch) {
                                            Helpers.sendMessage(GroupsActivity.this, contactsInGroup,
                                                    new DbObject(InviteToSharedAppFeedObj.TYPE,
                                                            InviteToSharedAppObj.json(packageName, arg)));
                                            GroupsActivity.this.startActivity(localLaunch);
                                        }
                                    });
                                }
                            });

                        final ActionItem invite = new ActionItem();
                        invite.setTitle("Invite Contacts");
                        invite.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent i = new Intent();
                                    i.setAction(PickContactsActivity.INTENT_ACTION_PICK_CONTACTS);
                                    i.putExtra(PickContactsActivity.INTENT_EXTRA_NFC_SHARE, ndefForGroup(g));
                                    GroupsActivity.this.startActivityForResult(
                                        i, REQUEST_INVITE_TO_GROUP);   
                                }
                            });

                    
                        QuickAction qa = new QuickAction(v);

                        qa.addActionItem(sendIM);
                        qa.addActionItem(startApp);
                        qa.addActionItem(invite);
                        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

                        qa.show();
                    }
                });
        }
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
        mGroup = Maybe.unknown();
    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    private final static int ADD_GROUP = 0;

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, ADD_GROUP, 0, "Add group");
        menu.add(0, 1, 0, "debug load");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case ADD_GROUP: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter group name:");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Group.create(GroupsActivity.this, input.getText().toString(), mHelper);
                    }
                });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();
            return true;
        }
        case 1: {
            Intent intent = new Intent().setClass(this, HandleGroupSessionActivity.class);
            intent.setData(Uri.parse("dungbeetle-group-session://suif.stanford.edu/dungbeetle/index.php?session=519e513d66bc89f4cbbfb1f127ae2c40&groupName=cs294s&key=WwBUcE4Rf8LKQebVfgsp9g%3D%3D"));
            startActivity(intent);
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

    private NdefMessage ndefForGroup(Group g) {
        return NdefFactory.fromUri(g.dynUpdateUri);
    }
}



