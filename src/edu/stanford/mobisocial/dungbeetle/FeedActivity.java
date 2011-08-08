package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;

import android.widget.Toast;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import edu.stanford.mobisocial.dungbeetle.model.DbActions;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.objects.FeedObj;
import edu.stanford.mobisocial.dungbeetle.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.objects.iface.Activator;

import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RemoteActivity;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;

import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.objects.ActivityPullObj;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;



public class FeedActivity extends RichListActivity implements OnItemClickListener{

    public static final String SERVICECMD = "com.android.music.musicservicecommand";
	public static final String CMDNAME = "command";
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDSTOP = "stop";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";

	private ObjectListCursorAdapter mObjects;
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
        if(intent.hasExtra("contactId")) {
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
		}

		mObjects = new ObjectListCursorAdapter(this, c);
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

            
            /*findViewById(R.id.voice)
            	.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Intent voiceintent = new Intent(ObjectsActivity.this, VoiceRecorderActivity.class);
                            voiceintent.putExtra("feedUri", feedUri.toString());
                            startActivity(voiceintent);
                        }
                    });*/

            final ImageView more = (ImageView)findViewById(R.id.more);
            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QuickAction qa = DbActions.getActions(FeedActivity.this, mFeedUri, v);
                        qa.show();
                    }
                });
            
            /*findViewById(R.id.publish)
            	.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            new AlertDialog.Builder(FeedActivity.this)
                            .setTitle("Attach")
                            .setItems(new String[] {
                                    "TapBoard",
                                    "Photo",
                                    "Application...",
                                    "Voice",
                                    "Feed"
                                }, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0: {
                                                doActivityForResult(FeedActivity.this, 
                                                        new RemoteActivity(FeedActivity.this, new RemoteActivity.ResultHandler() {
                                                            
                                                            @Override
                                                            public void onResult(String data) {
                                                                // TODO: move this inside RemoteActivity
                                                                // TODO: finish objectification:
                                                                // new FeedUpdater().sendToFeed(feedUri, PictureObj.fromJson(data));
                                                                DbObject obj = StatusObj.from(data);
                                                                Helpers.sendToFeed(FeedActivity.this, obj, mFeedUri);
                                                            }
                                                        }));
                                                break;
                                            }
                                            case 1: {
                                                doActivityForResult(
                                                    FeedActivity.this, 
                                                    new PhotoTaker(
                                                        FeedActivity.this, 
                                                        new PhotoTaker.ResultHandler() {
                                                            @Override
                                                            public void onResult(byte[] data) {
                                                                DbObject obj = PictureObj.from(data);
                                                                Helpers.sendToFeed(
                                                                    FeedActivity.this, obj, mFeedUri);
                                                            }
                                                        }, 200, false));
                                                break;
                                            }
                                        case 2: {
                                            AppReferenceObj.promptForApplication(
                                                    FeedActivity.this, new AppReferenceObj.Callback() {
                                                @Override
                                                public void onAppSelected(String pkg, String arg, Intent localLaunch) {
                                                    DbObject obj = new AppReference(pkg, arg);
                                                    Helpers.sendToFeed(FeedActivity.this, obj, mFeedUri);
                                                    startActivity(localLaunch);
                                                }
                                            });
                                            break;
                                        }
                                        case 3: {
                                            Intent voiceintent = new Intent(FeedActivity.this, VoiceRecorderActivity.class);
                                            voiceintent.putExtra("feedUri", mFeedUri.toString());
                                            startActivity(voiceintent);
                                            break;
                                        }
                                        case 4: {
                                            Group g = Group.create(FeedActivity.this);
                                            Helpers.sendToFeed(FeedActivity.this,
                                                    StatusObj.from("Welcome to " + g.name + "!"), Feed.uriForName(g.feedName));
                                            Helpers.sendToFeed(FeedActivity.this, FeedObj.from(g), mFeedUri);
                                            break;
                                        }
                                    }
                                }
                            }
                            ).create().show();
                        }
                });*/
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
            DbObject.bindView(v, FeedActivity.this, c, mContactCache);
        }
    }
    
    @Override
    public void finish() {
        super.finish();
        mContactCache.close();
    }

    private String getFeedObjectClause() {
    	String[] types = DbObjects.getRenderableTypes();
    	StringBuffer allowed = new StringBuffer();
    	for (String type : types) {
    		allowed.append(",'").append(type).append("'");
    	}
    	return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }

    
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        error.show();
    }


}


