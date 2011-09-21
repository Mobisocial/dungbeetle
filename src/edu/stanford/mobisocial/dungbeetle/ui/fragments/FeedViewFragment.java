package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PhotoQuickTakeActivity;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.VoiceQuickRecordActivity;
import edu.stanford.mobisocial.dungbeetle.feed.DbActions;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.obj.ObjActions;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.adapter.ObjectListCursorAdapter;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

/**
 * Shows a series of posts from a feed.
 */
public class FeedViewFragment extends ListFragment implements OnItemClickListener, OnScrollListener,
        OnEditorActionListener, TextWatcher, LoaderManager.LoaderCallbacks<Cursor>, KeyEvent.Callback {

    public static final String ARG_FEED_URI = "feed_uri";
    public static final String ARG_DUAL_PANE = "dual_pane";

    private boolean DBG = false;
    private ObjectListCursorAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private Uri mFeedUri;
    private ContactCache mContactCache;
    private EditText mStatusText;
    private ImageView mSendTextButton;
    private ImageView mSendObjectButton;
	private CursorLoader mLoader;
	
	private String feedName;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
        feedName = mFeedUri.getLastPathSegment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_feed_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mStatusText = (EditText)view.findViewById(R.id.status_text);
        mStatusText.setOnEditorActionListener(FeedViewFragment.this);
        mStatusText.addTextChangedListener(FeedViewFragment.this);

        mSendTextButton = (ImageView)view.findViewById(R.id.send_text);
        mSendTextButton.setVisibility(View.GONE);
        mSendTextButton.setOnClickListener(mSendStatus);

        mSendObjectButton = (ImageView)view.findViewById(R.id.more);
        mSendObjectButton.setOnClickListener(mSendObject);

        getListView().setOnItemClickListener(this);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemLongClickListener(mLongClickListener);
        getListView().setOnScrollListener(this);

        MusubiBaseActivity.getInstance().setOnKeyListener(this);
        // int color = Feed.colorFor(feedName, Feed.BACKGROUND_ALPHA);
        // getListView().setCacheColorHint(color);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DBG) Log.d(TAG, "Activity created: " + getActivity());
        mContactCache = new ContactCache(getActivity());
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mObjects.getItem(position);
    	Cursor cursor = getActivity().getContentResolver().query(DbObject.OBJ_URI,
            	new String[] { 
            		DbObject.JSON,
            		DbObject.RAW,
            	},
            	DbObject._ID + " = ?", new String[] {String.valueOf(c.getLong(0))}, null);
        if(!cursor.moveToFirst())
        	return;
        
        final String jsonSrc = cursor.getString(0);
        final byte[] raw = cursor.getBlob(1);
        cursor.close();

        if (HomeActivity.DBG) Log.i(TAG, "Clicked object: " + jsonSrc);
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(getActivity(), obj, raw);
            }
        }
        catch(JSONException e){
            Log.e(TAG, "Couldn't parse obj.", e);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContactCache.close();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	resetUnreadMessages();
    }
    
    private void resetUnreadMessages() {

        try {
	        ContentValues cv = new ContentValues();
	        cv.put(Group.NUM_UNREAD, 0);
	        
	        this.getActivity().getContentResolver().update(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/" + Group.TABLE), cv, Group.FEED_NAME+"='"+feedName+"'", null);
	        
	        this.getActivity().getContentResolver().notifyChange(Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist"), null);        
        }
        catch (Exception e) {
        	
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    
    	if(mLoader != null) {
    		mLoader.cancelLoad();
    	}

    	//the mObjects field is accessed by the background loader
    	synchronized (this) {
    		if (mObjects != null) {
    			((ObjectListCursorAdapter) mObjects).closeCursor();
    		}
    	}

    	resetUnreadMessages();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND ||
                actionId == EditorInfo.IME_ACTION_DONE) {
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

    private AdapterView.OnItemLongClickListener mLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            showMenuForObj(position);
            return true;
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mLoader = ObjectListCursorAdapter.queryObjects(getActivity(), mFeedUri);
        mLoader.loadInBackground();
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    	//the mObjects field is accessed by the ui thread as well
    	synchronized (this) {
            mObjects = new ObjectListCursorAdapter(getActivity(), cursor);
		}
        setListAdapter(mObjects);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    void showMenuForObj(int position) {
        Cursor c = (Cursor)mObjects.getItem(position);
    	Cursor cursor = getActivity().getContentResolver().query(DbObject.OBJ_URI,
            	new String[] { 
            		DbObject.JSON,
            		DbObject.RAW,
            		DbObject.TYPE,
            	},
            	DbObject._ID + " = ?", new String[] {String.valueOf(c.getLong(0))}, null);
        if(!cursor.moveToFirst())
        	return;
        
        final String type = cursor.getString(2);
        final String jsonSrc = cursor.getString(0);
        final byte[] raw = cursor.getBlob(1);
        cursor.close();

        final JSONObject json;
        try {
            json = new JSONObject(jsonSrc);
        } catch (JSONException e) {
            Log.e(TAG, "Error building dialog", e);
            return;
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ObjMenuDialogFragment.newInstance(type, json, raw);
        newFragment.show(ft, "dialog");
    }

    public static class ObjMenuDialogFragment extends DialogFragment {
        String mType;
        JSONObject mObj;
		byte[] mRaw;

        public static ObjMenuDialogFragment newInstance(String type, JSONObject obj, byte[] raw) {
            return new ObjMenuDialogFragment(type, obj, raw);
        }

        // Required by framework; fields populated from savedInstanceState.
        public ObjMenuDialogFragment() {
            
        }

        private ObjMenuDialogFragment(String type, JSONObject obj, byte[] raw) {
            mType = type;
            mObj = obj;
            mRaw = raw;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mType = savedInstanceState.getString("type");
                try {
                    mObj = new JSONObject(savedInstanceState.getString("obj"));
                } catch (JSONException e) {}
            }

            final DbEntryHandler dbType = DbObjects.forType(mType);
            final List<ObjAction> actions = new ArrayList<ObjAction>();
            for (ObjAction action : ObjActions.getObjActions()) {
                if (action.isActive(dbType, mObj)) {
                    actions.add(action);
                }
            }
            final String[] actionLabels = new String[actions.size()];
            int i = 0;
            for (ObjAction action : actions) {
                actionLabels[i++] = action.getLabel();
            }
            return new AlertDialog.Builder(getActivity()).setTitle("Handle...")
                    .setItems(actionLabels, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            actions.get(which).actOn(getActivity(), dbType, mObj, mRaw);
                        }
                    }).create();
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putString("type", mType);
            bundle.putString("obj", mObj.toString());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            event.startTracking();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            event.startTracking();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            Intent record = new Intent(getActivity(), VoiceQuickRecordActivity.class);
            record.putExtra("feed_uri", mFeedUri);
            startActivity(record);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        /*if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }*/
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Intent record = new Intent(getActivity(), VoiceQuickRecordActivity.class);
            record.putExtra("feed_uri", mFeedUri);
            record.putExtra("keydown", true);
            startActivity(record);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent camera = new Intent(getActivity(), PhotoQuickTakeActivity.class);
            camera.putExtra("feed_uri", mFeedUri);
            startActivity(camera);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            if (!event.isTracking()) {
                return true;
            }
            if (!event.isLongPress()) {
                AudioManager audio = (AudioManager)getActivity().getSystemService(
                        Context.AUDIO_SERVICE);
                audio.adjustVolume(AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
                return true;
            }
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!event.isTracking()) {
                return true;
            }
            if (!event.isLongPress()) {
                AudioManager audio = (AudioManager)getActivity().getSystemService(
                        Context.AUDIO_SERVICE);
                audio.adjustVolume(AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
                return true;
            }
        }
        return false;
    }

	@Override
	public void onScroll(AbsListView view, int firstVisible,
			int visibleCount, int totalCount) {
 		if(mObjects == null)
			return;		
		
		boolean loadMore = /* maybe add a padding */
	            firstVisible + visibleCount >= mObjects.getTotalQueried();

    	if (loadMore) {
    		Log.w(TAG, "load more");
    		mLoader.cancelLoad();
    		Activity activity = getActivity();
    		if(activity != null) {
        		mLoader = mObjects.queryLaterObjects(activity, mFeedUri, totalCount);
    		}
    	}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}
}
