package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.content.ContentValues;
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
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.DbObjects;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppObj;
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



public class ObjectsActivity extends RichListActivity implements OnItemClickListener{

	private ObjectListCursorAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private String feedName = "friend";
    private Uri feedUri;
    private ContactCache mContactCache;


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c;
        Intent intent = getIntent();
        mContactCache = new ContactCache(this);
        
        if(intent.hasExtra("group_id")) {
        	try {
	        	Long groupId = intent.getLongExtra("group_id", -1);
	        	feedName = new DBHelper(this).groupForGroupId(groupId).get().feedName;
        	} catch (Maybe.NoValError e) {
        		Log.w(TAG, "Tried to view a group with bad group id");
        	}
        } else if (intent.hasExtra("feed_id")) {
            feedName = intent.getStringExtra("feed_id");
        }
        
        feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName);
        if(intent.hasExtra("contactId")) {
            Long contactId = intent.getLongExtra("contact_id", -1);
            c = getContentResolver().query(
                feedUri,
                null, 
                getFeedObjectClause() + " AND " + DbObject.CONTACT_ID + "=?", new String[]{ 
                    String.valueOf(contactId) }, 
                DbObject._ID + " DESC");
		}
        else {
            c = getContentResolver().query(
                feedUri,
                null, 
                getFeedObjectClause(), null,
                DbObject._ID + " DESC");
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
                                               StatusObj.from(update), 
                                               feedUri);
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

            
            findViewById(R.id.publish)
            	.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            new AlertDialog.Builder(ObjectsActivity.this)
                            .setTitle("Choose App")
                            .setItems(new String[] {
                                    "TapBoard",
                                    "PhotoTaker",
                                    "Application...",
                                    "VoiceRecorder"
                                }, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0: {
                                                doActivityForResult(ObjectsActivity.this, 
                                                        new RemoteActivity(ObjectsActivity.this, new RemoteActivity.ResultHandler() {
                                                            
                                                            @Override
                                                            public void onResult(String data) {
                                                                // TODO: move this inside RemoteActivity
                                                                // TODO: finish objectification:
                                                                // new FeedUpdater().sendToFeed(feedUri, PictureObj.fromJson(data));
                                                                DbObject obj = StatusObj.from(data);
                                                                Helpers.sendToFeed(ObjectsActivity.this, obj, feedUri);
                                                            }
                                                        }));
                                                break;
                                            }
                                            case 1: {
                                                doActivityForResult(
                                                    ObjectsActivity.this, 
                                                    new PhotoTaker(
                                                        ObjectsActivity.this, 
                                                        new PhotoTaker.ResultHandler() {
                                                            @Override
                                                            public void onResult(byte[] data) {
                                                                DbObject obj = PictureObj.from(data);
                                                                Helpers.sendToFeed(
                                                                    ObjectsActivity.this, obj, feedUri);
                                                            }
                                                        }, 200, false));
                                                break;
                                            }
                                        case 2: {
                                            InviteToSharedAppObj.promptForApplication(
                                                    ObjectsActivity.this, new InviteToSharedAppObj.Callback() {
                                                @Override
                                                public void onAppSelected(String pkg, String arg, Intent localLaunch) {
                                                    DbObject obj = InviteToSharedAppObj.from(pkg, arg);
                                                    Helpers.sendToFeed(
                                                        ObjectsActivity.this, obj, feedUri);
                                                }
                                            });
                                            break;
                                        }
                                        case 3: {
                                            Intent voiceintent = new Intent(ObjectsActivity.this, VoiceRecorderActivity.class);
                                            voiceintent.putExtra("feedUri", feedUri.toString());
                                            startActivity(voiceintent);
                                            break;
                                        }
                                    }
                                }
                            }
                            ).create().show();
                        }
                });
        }
        else{
            findViewById(R.id.add_object_button).setVisibility(View.GONE);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(ObjectsActivity.this, obj);
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
            DbObject.bindView(v, ObjectsActivity.this, c, mContactCache);
        }
    }
    
    @Override
    public void finish() {
        super.finish();
        mContactCache.close();
    }

    private String getFeedObjectClause() {
        // TODO: Enumerate all Object classes, look for FeedRenderables.

    	String[] types = DbObjects.getRenderableTypes();
    	StringBuffer allowed = new StringBuffer();
    	for (String type : types) {
    		allowed.append(",'").append(type).append("'");
    	}
    	return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }


}


