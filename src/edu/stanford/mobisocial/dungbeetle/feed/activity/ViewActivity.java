package edu.stanford.mobisocial.dungbeetle.feed.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
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

public class ViewActivity extends RichListActivity
        implements OnItemClickListener, OnEditorActionListener, TextWatcher {

    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

	private ListAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private String feedName = null;
    private Uri mFeedUri;
    private ContactCache mContactCache;
    private EditText mStatusText;
    private ImageView mSendTextButton;
    private ImageView mSendObjectButton;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_view);

		mStatusText = (EditText)findViewById(R.id.status_text);
		mStatusText.setOnEditorActionListener(ViewActivity.this);
		mStatusText.addTextChangedListener(ViewActivity.this);

		mSendTextButton = (ImageView)findViewById(R.id.send_text);
        mSendTextButton.setVisibility(View.GONE);
        mSendTextButton.setOnClickListener(mSendStatus);

        mSendObjectButton = (ImageView)findViewById(R.id.more);
        mSendObjectButton.setOnClickListener(mSendObject);

        Intent intent = getIntent();
        mContactCache = new ContactCache(this);
        if(feedName == null && intent.hasExtra("group_id")) {
        	try {
	        	Long groupId = intent.getLongExtra("group_id", -1);
	            DBHelper dbHelper = new DBHelper(this);
	            feedName = dbHelper.groupForGroupId(groupId).get().feedName;
	            dbHelper.close();
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

        FeedProcessor processor = new DefaultFeedProcessor(mContactCache);
		mObjects = processor.getListAdapter(this, mFeedUri);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
		getListView().setFastScrollEnabled(true);
		//getListView().setCacheColorHint(color);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        if (DungBeetleActivity.DBG) Log.i(TAG, "Clicked object: " + jsonSrc);
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(mFeedUri, ViewActivity.this, obj);
            }
        }
        catch(JSONException e){
            Log.e(TAG, "Couldn't parse obj.", e);
        }
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

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            mSendStatus.onClick(v);
        }
        return true;
    }

    private final View.OnClickListener mSendStatus = new OnClickListener() {
        public void onClick(View v) {
            Editable editor = mStatusText.getText();
            String update = editor.toString();
            if(update.length() != 0){
                editor.clear();
                Helpers.sendToFeed(ViewActivity.this,
                        StatusObj.from(update), mFeedUri);
            }
            InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mStatusText.getWindowToken(), 0);
        }
    };

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0) {
            mSendTextButton.setVisibility(View.GONE);
            mSendObjectButton.setVisibility(View.VISIBLE);
        } else {
            mSendTextButton.setVisibility(View.VISIBLE);
            mSendObjectButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
        
    }

    private View.OnClickListener mSendObject = new OnClickListener() {
        @Override
        public void onClick(View v) {
            QuickAction qa = DbActions.getActions(ViewActivity.this, mFeedUri, v);
            qa.show();
        }
    };
}


