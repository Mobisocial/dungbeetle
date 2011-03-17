package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import edu.stanford.mobisocial.util.BitmapManager;
import android.widget.ImageView;
import android.content.DialogInterface;
import android.widget.EditText;
import android.app.AlertDialog;
import android.view.MenuItem;
import android.view.Menu;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.widget.CursorAdapter;
import android.net.Uri;
import android.database.Cursor;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;


public class ContactsActivity extends ListActivity implements OnItemClickListener{

	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mgr = new BitmapManager(10);

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            new String[]{Contact._ID, Contact.NAME, Contact.EMAIL, Contact.PUBLIC_KEY}, 
            null, null, null);
		mContacts = new ContactListCursorAdapter(this, c);
		setListAdapter(mContacts);
		getListView().setOnItemClickListener(this);
	}


    public void onItemClick(AdapterView<?> parent, View view, int position, long id){}

    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            String name = c.getString(c.getColumnIndex(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }

            String email = c.getString(c.getColumnIndex(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mgr.lazyLoadImage(icon, Gravatar.gravatarUri(email));            
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndex(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }

            String email = c.getString(c.getColumnIndex(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mgr.lazyLoadImage(icon, Gravatar.gravatarUri(email));            
        }

    }


    private final static int UPDATE_CONTACT = 0;

	public boolean onCreateOptionsMenu(Menu menu){
		return true;
	}

	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		menu.clear();
		menu.add(0, 0, 0, "Set email (debug)");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case UPDATE_CONTACT: {
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
		default: return false;
		}
	}


}



