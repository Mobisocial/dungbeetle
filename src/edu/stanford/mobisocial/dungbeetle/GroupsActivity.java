package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import java.util.Collection;


public class GroupsActivity extends ListActivity implements OnItemClickListener{
	private GroupListCursorAdapter mGroups;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mBitmaps = new BitmapManager(10);
	private DBHelper mHelper;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        mHelper = new DBHelper(this);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"),
            null, null, null, null);
		mGroups = new GroupListCursorAdapter(this, c);
		setListAdapter(mGroups);
		getListView().setOnItemClickListener(this);
	}


    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
        Cursor cursor = (Cursor)mGroups.getItem(position);
        final Group g = new Group(cursor);
        final Collection<Contact> contactsInGroup = g.contactCollection(mHelper);
        final CharSequence[] items = new CharSequence[]{ "Send Message", "Start Application", "View Contacts", "Delete" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actions");
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item){
                    case 0:
                        UIHelpers.sendMessageToContact(
                            GroupsActivity.this, 
                            contactsInGroup);
                        break;
                    case 1:
                        UIHelpers.startApplicationWithContact(
                            GroupsActivity.this, 
                            contactsInGroup);
                        break;
                    case 2:
                        Intent viewGroupIntent = new Intent(GroupsActivity.this, ContactsActivity.class);
                        viewGroupIntent.putExtra("group_id", g.id);
                        startActivity(viewGroupIntent);
                    	break;
                    case 3:
                        Helpers.deleteGroup(GroupsActivity.this, g.id);
                        break;
                    }
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private class GroupListCursorAdapter extends CursorAdapter {
        public GroupListCursorAdapter (Context context, Cursor c) {
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
            String name = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }           
        }
    }


	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

    private final static int ADD_GROUP = 0;
    private final static int WRITE_GROUP_TO_TAG = 1;


	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		menu.clear();
		menu.add(0, ADD_GROUP, 0, "Add group");
		menu.add(0, WRITE_GROUP_TO_TAG, 0, "Write dynamic group to tag");
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
                        ContentValues values = new ContentValues();
                        values.put(Group.NAME, input.getText().toString());
                        GroupsActivity.this.getContentResolver().insert(
                            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), values);
                    }
                });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();
			return true;
		}
		case WRITE_GROUP_TO_TAG: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter group name:");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        IdentityProvider ident = new DBIdentityProvider(mHelper);
                        Uri uri = GroupProviders.newSessionUri(ident, input.getText().toString());
                        ((DungBeetleActivity)getParent()).writeGroupToTag(uri);
                    }
                });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();

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

}



