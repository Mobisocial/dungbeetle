package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;


public class GroupsActivity extends ListActivity implements OnItemClickListener{

	private GroupListCursorAdapter mGroups;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mBitmaps = new BitmapManager(10);

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
            new String[]{ Group._ID, Group.NAME }, 
            null, null, null);
		mGroups = new GroupListCursorAdapter(this, c);
		setListAdapter(mGroups);
		getListView().setOnItemClickListener(this);
	}

    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
    	Intent viewGroupIntent = new Intent(this, ContactsActivity.class);
    	Cursor c = (Cursor)mGroups.getItem(position);
    	Long group_id = c.getLong(c.getColumnIndexOrThrow(Group._ID));
    	viewGroupIntent.putExtra("group_id", group_id);
		startActivity(viewGroupIntent);
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


    private final static int UPDATE_GROUP = 0;

	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		menu.clear();
		menu.add(0, 0, 0, "Add group");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case UPDATE_GROUP: {
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
		default: return false;
		}
	}

    @Override
    public void finish() {
        super.finish();
    }

}



