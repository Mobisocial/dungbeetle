package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.objects.ObjectReceiver;
import edu.stanford.mobisocial.dungbeetle.objects.ObjectReceiverManager;
import edu.stanford.mobisocial.dungbeetle.objects.StatusUpdate;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;



public class ObjectsActivity extends ListActivity implements OnItemClickListener{

	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private ObjectListCursorAdapter mObjects;
	private DBIdentityProvider mIdent;
	private DBHelper mHelper;
	private static final int REQUEST_STATUS = 98424;
	public static final String ACTION_UPDATE_STATUS = "mobisocial.db.action.UPDATE_STATUS";
	
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);

        Cursor c;
        
        Intent intent = getIntent();
        if(intent.hasExtra("contact_id")) {
            String contact_id = Long.toString(intent.getLongExtra("contact_id", -1));

            
            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"),
                null, 
                Object.TYPE + "=? AND " + Object.CONTACT_ID + "=?", new String[]{ "status" , contact_id}, 
                Object._ID + " DESC");
		}

        else if(intent.hasExtra("group_id")) {
            String contact_id = Long.toString(intent.getLongExtra("contact_id", -1));

            
            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"),
                null, 
                Object.CONTACT_ID + "=?", new String[]{ contact_id}, 
                Object._ID + " DESC");
		}
		
        
		else {
            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"),
                null, 
                null, null, 
                Object._ID + " DESC");
		}
		
		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
		getListView().setFastScrollEnabled(true);

        mHelper = new DBHelper(ObjectsActivity.this); 
        mIdent = new DBIdentityProvider(mHelper);

        if(!intent.hasExtra("contact_id") && !intent.hasExtra("group_id"))
        {
		    Button button = (Button)findViewById(R.id.add_object_button);
		    button.setOnClickListener(new OnClickListener() {
				    public void onClick(View v) {
				    	Intent update = new Intent(ACTION_UPDATE_STATUS);
                        Intent chooser = Intent.createChooser(update, "Update status");
                        startActivityForResult(chooser, REQUEST_STATUS);
				    }
			    });
	    }
	    else
	    {
	        findViewById(R.id.add_object_button).setVisibility(View.GONE);
	    }
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id){}


    // Implement a little cache so we don't have to keep pulling the same
    // contacts. Would be nice to pre-warm this cache given a list of 
    // person ids...
    private Map<Long, Contact> mContactCache = new HashMap<Long, Contact>();
    private Contact getContact(Long id){
        if(mContactCache.containsKey(id)){
            return mContactCache.get(id);
        }
        else{
            if(id.equals(Contact.MY_ID)){
                Contact contact = new Contact(
                    Contact.MY_ID,
                    mIdent.userPersonId(),
                    mIdent.userName(), 
                    mIdent.userEmail(),
                    0,
                    "");
                mContactCache.put(id, contact);
                return contact;
            }
            else{
                Cursor c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
                    null, Contact._ID + "=?", 
                    new String[]{String.valueOf(id)}, null);
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
            Long contactId = c.getLong(c.getColumnIndexOrThrow(
                                           Object.CONTACT_ID));
            Contact contact = getContact(contactId);
            if(contact != null){
                TextView nameText = (TextView) v.findViewById(R.id.name_text);
                //String email = contact.email == null ? "NA" : contact.email;
                //email = obj.optString("name");
                nameText.setText("Name: " + contact.name);
                final ImageView icon = (ImageView)v.findViewById(R.id.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mBitmaps.lazyLoadImage(icon, Gravatar.gravatarUri(contact.email));

            }
            
            // TODO: update child view
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View frame = inflater.inflate(R.layout.status_entry, (ViewGroup)v.findViewById(R.id.contact_frame));
            try {
            	JSONObject content = new JSONObject(jsonSrc);
            	for (ObjectReceiver receiver : getReceivers()) {
            		if (receiver.handlesObject(content)) {
            			receiver.render(frame, content);
            			return;
            		}
            	}
            } catch (JSONException e) {
            	Log.e("db", "error opening json");
            }
        }
    }
    
    private List<ObjectReceiver> mObjectReceivers = null;
    private List<ObjectReceiver> getReceivers() {
    	if (mObjectReceivers == null) {
    		mObjectReceivers = ObjectReceiverManager.getDefaults();
    	}
    	return mObjectReceivers;
    }
    
    @Override
    public void finish() {
        super.finish();
        mIdent.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_STATUS) {
    		if (resultCode == RESULT_OK) {
    			String update = data.getStringExtra(Intent.EXTRA_TEXT);
    			Helpers.updateStatus(ObjectsActivity.this, update);
    		}
    	}
    }
    
    private void toast(String text) {
    	Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}



