package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import java.util.HashMap;
import java.util.Map;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import android.content.DialogInterface;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.Button;
import org.json.JSONException;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.Object;
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
import android.view.View.OnClickListener;


public class ObjectsActivity extends ListActivity implements OnItemClickListener{

	protected final BitmapManager mgr = new BitmapManager(10);
	private ObjectListCursorAdapter mObjects;
	private DBIdentityProvider mIdent;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
            null, 
            Object.TYPE + "=?", new String[]{ "status" }, 
            Object.TIMESTAMP + " DESC");
		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
 
        mIdent = new DBIdentityProvider(new DBHelper(this));

		Button button = (Button)findViewById(R.id.add_object_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(ObjectsActivity.this);
                    alert.setMessage("Please enter your new status message:");
                    final EditText input = new EditText(ObjectsActivity.this);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String value = input.getText().toString();
                                Helpers.updateStatus(ObjectsActivity.this, value);
                            }
                        });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {}
                        });
                    alert.show();
				}
			});
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id){}


    // Implement a little cache so we don't have to keep pulling the same
    // contacts. Would be nice to pre-warm this cache given a list of 
    // person ids...
    private Map<String, Contact> mContactCache = new HashMap<String, Contact>();
    private Contact getContact(String id){
        if(mContactCache.containsKey(id)){
            return mContactCache.get(id);
        }
        else{
            if(id.equals(mIdent.userPersonId())){
                Contact contact = new Contact(id, 
                                              mIdent.userName(), 
                                              mIdent.userEmail());
                mContactCache.put(id, contact);
                return contact;
            }
            else{
                Cursor c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
                    null, Contact.PERSON_ID + "=?", new String[]{id}, null);
                c.moveToFirst();
                if(c.isAfterLast()){
                    return null;
                }
                else{
                    Contact contact = new Contact(c);
                    mContactCache.put(id, contact);
                    return contact;
                }
            }
        }
    }

    private class ObjectListCursorAdapter extends CursorAdapter {

        public ObjectListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.objects_item, parent, false);
            bindView(v, context, c);
            return v;
        }

        @Override
        public void bindView(View v, Context context, Cursor c) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
            String personId = c.getString(c.getColumnIndexOrThrow(
                                              Object.CREATOR_PERSON_ID));
            Contact contact = getContact(personId);
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String text = obj.optString("text");
                TextView bodyText = (TextView) v.findViewById(R.id.body_text);
                bodyText.setText(text);

                if(contact != null){
                    TextView nameText = (TextView) v.findViewById(R.id.name_text);
                    String email = contact.email == null ? "NA" : contact.email;
                    nameText.setText(email);
                    final ImageView icon = (ImageView)v.findViewById(R.id.icon);
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mgr.lazyLoadImage(icon, Gravatar.gravatarUri(contact.email));
                }

            }catch(JSONException e){}
        }

    }


}



