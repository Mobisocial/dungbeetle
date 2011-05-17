package edu.stanford.mobisocial.dungbeetle;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.objects.Activator;
import edu.stanford.mobisocial.dungbeetle.objects.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.objects.Objects;
import edu.stanford.mobisocial.dungbeetle.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RelativeDate;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;



public class ObjectsActivity extends RichListActivity implements OnItemClickListener{

	private ObjectListCursorAdapter mObjects;
	private DBIdentityProvider mIdent;
	private DBHelper mHelper;
	public static final String TAG = "ObjectsActivity";
    private String feedName = "friend";
    private Uri feedUri;
    private ContactCache mContactCache;


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c;
        mHelper = new DBHelper(ObjectsActivity.this); 
        mIdent = new DBIdentityProvider(mHelper);
        Intent intent = getIntent();
        mContactCache = new ContactCache();
        
        if(intent.hasExtra("group_id")) {
        	try {
	        	Long groupId = intent.getLongExtra("group_id", -1);
	        	Group group = mHelper.groupForGroupId(groupId).get();
	            feedName = group.feedName;
        	} catch (Maybe.NoValError e) {
        		Log.w(TAG, "Tried to view a group with bad group id");
        	}
        }
        
        feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName);
        if(intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contact_id", -1);
            c = getContentResolver().query(
                feedUri,
                null, 
                getFeedObjectClause() + " AND " + Object.CONTACT_ID + "=?", new String[]{ 
                    String.valueOf(contactId) }, 
                Object._ID + " DESC");
		}
        else {
            c = getContentResolver().query(
                feedUri,
                null, 
                getFeedObjectClause(), null,
                Object._ID + " DESC");
		}

		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
		getListView().setFastScrollEnabled(true);

		// TODO: Get rid of this? All feeds are created equal! -BJD
        if(!intent.hasExtra("contact_id")){
            Button button = (Button)findViewById(R.id.add_object_button);
            button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        EditText ed = (EditText)findViewById(R.id.status_text);
                    	Editable editor = ed.getText();
                    	String update = editor.toString();
                        if(update.length() != 0){
                            Helpers.sendToFeed(ObjectsActivity.this, 
                                               StatusObj.getStatusObj(update), 
                                               feedUri);
                            editor.clear();
                        }
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
                    }
                });
            
            findViewById(R.id.publish)
            	.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            doActivityForResult(
                                ObjectsActivity.this, 
                                new PhotoTaker(
                                    ObjectsActivity.this, 
                                    new PhotoTaker.ResultHandler() {
                                        @Override
                                        public void onResult(byte[] data) {
                                            ContentValues values = new ContentValues();
                                            JSONObject obj = PictureObj.json(data);
                                            values.put(Object.JSON, obj.toString());
                                            values.put(Object.TYPE, PictureObj.TYPE);
                                            Helpers.sendToFeed(
                                                ObjectsActivity.this, values, feedUri);
                                        }
                                    }, 200, false));
                        }
                    });
        }
        else{
            findViewById(R.id.add_object_button).setVisibility(View.GONE);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = Objects.getActivator(obj);
            if(activator != null){
                activator.activate(ObjectsActivity.this, obj);
            }
        }
        catch(JSONException e){
            Log.e(TAG, "Couldn't parse obj.", e);
        }
        Log.i(TAG, "Clicked object: " + jsonSrc);
    }


    private class ContactCache extends ContentObserver{

        public ContactCache(){
            super(new Handler(ObjectsActivity.this.getMainLooper()));
            ObjectsActivity.this.getContentResolver().registerContentObserver(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
                true, this);

            // So we pick up changes to user's profile image..
            ObjectsActivity.this.getContentResolver().registerContentObserver(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/my_info"),
                true, this);
        }
        
        private Map<Long, Contact> mContactCache = new HashMap<Long, Contact>();

        @Override
        public void onChange(boolean self){
            mContactCache.clear();
        }

        private Maybe<Contact> getContact(long id){
            if(mContactCache.containsKey(id)){
                return Maybe.definitely(mContactCache.get(id));
            }
            else{
                if(id == Contact.MY_ID){
                    Contact contact = mIdent.contactForUser();
                    mContactCache.put(id, contact);
                    return Maybe.definitely(contact);
                }
                else{
                    Cursor c = getContentResolver().query(
                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
                        null, Contact._ID + "=?", 
                        new String[]{String.valueOf(id)}, null);
                    c.moveToFirst();
                    if(c.isAfterLast()){
                        return Maybe.unknown();
                    }
                    else{
                        Contact contact = new Contact(c);
                        mContactCache.put(id, contact);
                        return Maybe.definitely(contact);
                    }
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
            Long contactId = c.getLong(c.getColumnIndexOrThrow(Object.CONTACT_ID));
            Long timestamp = c.getLong(c.getColumnIndexOrThrow(Object.TIMESTAMP));
            Date date = new Date(timestamp);
            try{
                Contact contact = mContactCache.getContact(contactId).get();

                TextView nameText = (TextView) v.findViewById(R.id.name_text);
                nameText.setText(contact.name);

                final ImageView icon = (ImageView)v.findViewById(R.id.icon);
                ((App)getApplication()).contactImages.lazyLoadContactPortrait(contact, icon);

                try {
                    JSONObject content = new JSONObject(jsonSrc);

                    TextView timeText = (TextView)v.findViewById(R.id.time_text);
                    timeText.setText(RelativeDate.getRelativeDate(date));

                    ViewGroup frame = (ViewGroup)v.findViewById(R.id.object_content);
                    frame.removeAllViews();

                    FeedRenderer renderer = Objects.getFeedRenderer(content);
                    if(renderer != null){
                        renderer.render(ObjectsActivity.this, frame, content);
                    }
                } catch (JSONException e) {
                    Log.e("db", "error opening json");
                }
            }
            catch(Maybe.NoValError e){}
        }
    }
    
    @Override
    public void finish() {
        super.finish();
        mIdent.close();
    }

    public String getFeedObjectClause() {
    	String[] types = new String[] { StatusObj.TYPE, ProfilePictureObj.TYPE, PictureObj.TYPE };
    	StringBuffer allowed = new StringBuffer();
    	for (String type : types) {
    		allowed.append(",'").append(type).append("'");
    	}
    	return Object.TYPE + " in (" + allowed.substring(1) + ")";
    }


}



