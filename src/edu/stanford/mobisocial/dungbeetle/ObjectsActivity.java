package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
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
import android.widget.TextView;
import android.graphics.BitmapFactory;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.objects.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.objects.Objects;
import edu.stanford.mobisocial.dungbeetle.objects.PictureObj.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;



public class ObjectsActivity extends ListActivity implements OnItemClickListener{

	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private ObjectListCursorAdapter mObjects;
	private DBIdentityProvider mIdent;
	private DBHelper mHelper;
	public static final String TAG = "ObjectsActivity";
    private String feedName = "friend";
	
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c;
        mHelper = new DBHelper(ObjectsActivity.this); 
        mIdent = new DBIdentityProvider(mHelper);
        Intent intent = getIntent();
        if(intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contact_id", -1);
            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName),
                null, 
                getFeedObjectClause() + " AND " + Object.CONTACT_ID + "=?", new String[]{ 
                    String.valueOf(contactId) }, 
                Object._ID + " DESC");
		}
        else if(intent.hasExtra("group_id")) {
            Long groupId = intent.getLongExtra("group_id", -1);
            try{
                Group group = mHelper.groupForGroupId(groupId).get();
                feedName = group.feedName;
                c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName),
                    null, 
                    getFeedObjectClause(), null, 
                    Object._ID + " DESC");                
            }
            catch(Maybe.NoValError e){
                c = new MatrixCursor(new String[]{});
            }
		}
		else {
            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName),
                null, 
                getFeedObjectClause(), null, 
                Object._ID + " DESC");
		}
		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
		getListView().setFastScrollEnabled(true);

        if(!intent.hasExtra("contact_id")){
            Button button = (Button)findViewById(R.id.add_object_button);
            button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	Editable editor = ((EditText)findViewById(R.id.status_text)).getText();
                    	String update = editor.toString();
                    	Helpers.updateStatus(ObjectsActivity.this, feedName, update);
                    	editor.clear();
                    }
                });
            
            findViewById(R.id.publish)
            	.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	// TODO: QuickActions or similar UI.
                    	doActivityForResult(ObjectsActivity.this, new PhotoTaker(ObjectsActivity.this));
                    }
                });
        }
        else{
            findViewById(R.id.add_object_button).setVisibility(View.GONE);
        }
    }
    
    private static int ACTIVITY_CALLOUT = 39472874;
    private static ActivityCallout mCurrentCallout;
    public static void doActivityForResult(Activity me, ActivityCallout callout) {
    	mCurrentCallout = callout;
    	Intent launch = callout.getStartIntent();
    	me.startActivityForResult(launch, ACTIVITY_CALLOUT);
    }
    
    public interface ActivityCallout {
    	public Intent getStartIntent();
    	public void handleResult(int resultCode, Intent data);
    }
    
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
        Log.i(TAG, "Clicked object: " + jsonSrc);
    }


    // Implement a little cache so we don't have to keep pulling the same
    // contacts. Would be nice to pre-warm this cache given a list of 
    // person ids...
    private Map<Long, Contact> mContactCache = new HashMap<Long, Contact>();
    private Maybe<Contact> getContact(Long id){
        if(mContactCache.containsKey(id)){
            return Maybe.definitely(mContactCache.get(id));
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
            try{
                Contact contact = getContact(contactId).get();
                TextView nameText = (TextView) v.findViewById(R.id.name_text);
                nameText.setText(contact.name);
                final ImageView icon = (ImageView)v.findViewById(R.id.icon);
                
                if(contact.picture != null) {
                    icon.setImageBitmap(BitmapFactory.decodeByteArray(contact.picture, 0, contact.picture.length));
                }
                try {
                    ViewGroup frame = (ViewGroup)v.findViewById(R.id.object_content);
                    frame.removeAllViews();
                    JSONObject content = new JSONObject(jsonSrc);
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
    	String[] types = new String[] { StatusObj.TYPE, ProfilePictureObj.TYPE };
    	StringBuffer allowed = new StringBuffer();
    	for (String type : types) {
    		allowed.append(",'").append(type).append("'");
    	}
    	return Object.TYPE + " in (" + allowed.substring(1) + ")";
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_CALLOUT) {
    		mCurrentCallout.handleResult(resultCode, data);
    	}
    }
}



