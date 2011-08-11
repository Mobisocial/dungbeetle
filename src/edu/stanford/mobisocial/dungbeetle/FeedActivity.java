package edu.stanford.mobisocial.dungbeetle;

import android.widget.Toast;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import edu.stanford.mobisocial.dungbeetle.feed.DbActions;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedProcessor;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.feed.processor.DefaultFeedProcessor;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;

import android.widget.ImageView;

public class FeedActivity extends RichListActivity implements OnItemClickListener{

    public static final String SERVICECMD = "com.android.music.musicservicecommand";
	public static final String CMDNAME = "command";
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDSTOP = "stop";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";

	private ListAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private String feedName = null;
    private Uri mFeedUri;
    private ContactCache mContactCache;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
		
        Cursor c;
        Intent intent = getIntent();
        mContactCache = new ContactCache(this);
        if(feedName == null && intent.hasExtra("group_id")) {
        	try {
	        	Long groupId = intent.getLongExtra("group_id", -1);
	        	feedName = new DBHelper(this).groupForGroupId(groupId).get().feedName;
        	} catch (Maybe.NoValError e) {
        		Log.w(TAG, "Tried to view a group with bad group id");
        	}
        } else if (intent.hasExtra("feed_id")) {
            feedName = intent.getStringExtra("feed_id");
        }
        if (feedName == null) {
            feedName = "friend";
        }

        int color = Feed.colorFor(feedName, Feed.BACKGROUND_ALPHA);
        mFeedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName);
        /*if(intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contact_id", -1);
            c = getContentResolver().query(
                mFeedUri,
                null, 
                getFeedObjectClause() + " AND " + DbObject.CONTACT_ID + "=?", new String[]{ 
                    String.valueOf(contactId) }, 
                DbObject._ID + " DESC");
		}
        else {
            c = getContentResolver().query(
                mFeedUri,
                null, 
                getFeedObjectClause(), null,
                DbObject._ID + " DESC");
		}*/

        FeedProcessor processor = new DefaultFeedProcessor(mContactCache);
		mObjects = processor.getListAdapter(this, mFeedUri);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
		getListView().setFastScrollEnabled(true);
		//getListView().setCacheColorHint(color);

		// TODO: Get rid of this? All feeds are created equal! -BJD
        if(!intent.hasExtra("contact_id")){
            ImageView addObject = (ImageView)findViewById(R.id.add_object);
            addObject.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        EditText ed = (EditText)findViewById(R.id.status_text);
                    	Editable editor = ed.getText();
                    	String update = editor.toString();
                        if(update.length() != 0){
                            Helpers.sendToFeed(FeedActivity.this, 
                                               StatusObj.from(update), 
                                               mFeedUri);
                            editor.clear();
                        }
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
                    }
                });

            final ImageView more = (ImageView)findViewById(R.id.more);
            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QuickAction qa = DbActions.getActions(FeedActivity.this, mFeedUri, v);
                        qa.show();
                    }
                });
        }
        else{
            findViewById(R.id.add_object).setVisibility(View.GONE);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(mFeedUri, FeedActivity.this, obj);
            }
        }
        catch(JSONException e){
            Log.e(TAG, "Couldn't parse obj.", e);
        }
        Log.i(TAG, "Clicked object: " + jsonSrc);
    }

    @Override
    public void finish() {
        super.finish();
        mContactCache.close();
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        error.show();
    }


}


