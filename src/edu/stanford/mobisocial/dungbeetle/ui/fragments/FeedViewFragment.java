package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import edu.stanford.mobisocial.dungbeetle.DBHelper;
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
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

public class FeedViewFragment extends ListFragment
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_feed_view, container, false);

		mStatusText = (EditText)view.findViewById(R.id.status_text);
		mStatusText.setOnEditorActionListener(FeedViewFragment.this);
		mStatusText.addTextChangedListener(FeedViewFragment.this);

		mSendTextButton = (ImageView)view.findViewById(R.id.send_text);
        mSendTextButton.setVisibility(View.GONE);
        mSendTextButton.setOnClickListener(mSendStatus);

        mSendObjectButton = (ImageView)view.findViewById(R.id.more);
        mSendObjectButton.setOnClickListener(mSendObject);

		return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        mContactCache = new ContactCache(getActivity());
        if (feedName == null && intent.hasExtra("group_id")) {
            try {
                Long groupId = intent.getLongExtra("group_id", -1);
                DBHelper dbHelper = new DBHelper(getActivity());
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

        //int color = Feed.colorFor(feedName, Feed.BACKGROUND_ALPHA);
        mFeedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName);

        FeedProcessor processor = new DefaultFeedProcessor(mContactCache);
        mObjects = processor.getListAdapter(getActivity(), mFeedUri);
        setListAdapter(mObjects);
        getListView().setOnItemClickListener(this);
        getListView().setFastScrollEnabled(true);
        //getListView().setCacheColorHint(color);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        if (HomeActivity.DBG) Log.i(TAG, "Clicked object: " + jsonSrc);
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(mFeedUri, getActivity(), obj);
            }
        }
        catch(JSONException e){
            Log.e(TAG, "Couldn't parse obj.", e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContactCache.close();
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
                Helpers.sendToFeed(getActivity(),
                        StatusObj.from(update), mFeedUri);
            }
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
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
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    private View.OnClickListener mSendObject = new OnClickListener() {
        @Override
        public void onClick(View v) {
            QuickAction qa = DbActions.getActions(getActivity(), mFeedUri, v);
            qa.show();
        }
    };
}