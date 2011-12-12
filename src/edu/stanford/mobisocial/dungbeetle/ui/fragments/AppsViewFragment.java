package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.PhotoQuickTakeActivity;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.VoiceQuickRecordActivity;
import edu.stanford.mobisocial.dungbeetle.feed.DbActions;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.action.LaunchApplicationAction;
import edu.stanford.mobisocial.dungbeetle.feed.action.LaunchApplicationAction.MusubiApp;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Filterable;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppStateObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.ObjActions;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.adapter.ObjectListCursorAdapter;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

/**
 * Shows a series of posts from a feed.
 */
public class AppsViewFragment extends ListFragment implements OnScrollListener,
        OnEditorActionListener, TextWatcher, LoaderManager.LoaderCallbacks<Cursor>, KeyEvent.Callback {

    public static final String ARG_FEED_URI = "feed_uri";
    public static final String ARG_DUAL_PANE = "dual_pane";

    private boolean DBG = true;
    private ObjectListCursorAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private Uri mFeedUri;
    private EditText mStatusText;
    private ImageView mSendTextButton;
    private ImageView mSendObjectButton;
	private CursorLoader mLoader;

    private OnClickListener newApp = new OnClickListener() {
        @Override
        public void onClick(View v) {
            LaunchApplicationAction.promptForApplication(getActivity(), mFeedUri);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
        if (DBG) Log.d(TAG, "Attaching fragment to feed " + mFeedUri);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_apps_view, container, false);
		view.findViewById(R.id.new_app).setOnClickListener(newApp);

		// TODO: I didn't like this, but maybe with some work it could look ok
		/*
		Gallery appbar = (Gallery)view.findViewById(R.id.app_list);
		appbar.setAdapter(new AppSpinner(getActivity()));
		*/
		return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener(mItemClickListener);
        lv.setOnItemLongClickListener(mItemLongClickListener);
        lv.setOnScrollListener(this);
        lv.setFocusable(true);

        MusubiBaseActivity.getInstance().setOnKeyListener(this);
        // int color = Feed.colorFor(feedName, Feed.BACKGROUND_ALPHA);
        // getListView().setCacheColorHint(color);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DBG) Log.d(TAG, "Activity created: " + getActivity());
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: Not sure why this was added, but it causes massive slowdown
        // Discuss with bjd before uncommenting
        // getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onPause() {
    	super.onPause();
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	List<String> filterTypes = new ArrayList<String>();
    	filterTypes.add(AppObj.TYPE);
    	filterTypes.add(AppStateObj.TYPE);
    	mLoader = ObjectListCursorAdapter.queryObjects(getActivity(), mFeedUri, filterTypes.toArray(new String[filterTypes.size()]));
        

        mLoader.loadInBackground();
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    	//the mObjects field is accessed by the ui thread as well
        if (mObjects == null) {
            mObjects = new ObjectListCursorAdapter(getActivity(), cursor);
            setListAdapter(mObjects);
		} else {
		    mObjects.changeCursor(cursor);
		}
    	Log.w(TAG, "setting adapter");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    void showMenuForObj(int position) {
    	//this first cursor is the internal one
        Cursor cursor = (Cursor)mObjects.getItem(position);
        long objId = cursor.getLong(0);

        DbObj obj = App.instance().getMusubi().objForId(objId);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ObjMenuDialogFragment.newInstance(obj);
        newFragment.show(ft, "dialog");
    }

    public static class ObjMenuDialogFragment extends DialogFragment {
        String mType;
        private DbObj mObj;
        private JSONObject mJson;
		byte[] mRaw;
		Uri mFeedUri;
		long mHash;
		long mContactId;

        public static ObjMenuDialogFragment newInstance(DbObj obj) {
            return new ObjMenuDialogFragment(obj);
        }

        // Required by framework; fields populated from savedInstanceState.
        public ObjMenuDialogFragment() {
            
        }

        private ObjMenuDialogFragment(DbObj obj) {
            loadFromObj(obj);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                long objId = savedInstanceState.getLong("objId");
                loadFromObj(App.instance().getMusubi().objForId(objId));
            }

            final DbEntryHandler dbType = DbObjects.forType(mType);
            final List<ObjAction> actions = new ArrayList<ObjAction>();
            for (ObjAction action : ObjActions.getObjActions()) {
                if (action.isActive(getActivity(), dbType, mJson)) {
                    actions.add(action);
                }
            }
            final String[] actionLabels = new String[actions.size()];
            int i = 0;
            for (ObjAction action : actions) {
                actionLabels[i++] = action.getLabel(getActivity());
            }
            return new AlertDialog.Builder(getActivity()).setTitle("Handle...")
                    .setItems(actionLabels, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "getting for " + getActivity());
                            actions.get(which).actOn(getActivity(), dbType, mObj);
                        }
                    }).create();
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putLong("objId", mObj.getLocalId());
        }

        private void loadFromObj(DbObj obj) {
            mFeedUri = obj.getContainingFeed().getUri();
            mType = obj.getType();
            mObj = obj;
            mJson = obj.getJson();
            mRaw = obj.getRaw();
            mHash = obj.getHash();
            mContactId = obj.getSender().getLocalId();
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
    		mLoader.cancelLoad();

        	if(getActivity() instanceof Filterable)
        	{
	    		Filterable context = (Filterable) getActivity();
	    		if(context != null) {
	    			List<String> filterTypes = new ArrayList<String>();
	    	    	for(int x = 0; x < context.getFilterTypes().length; x++) {
	    	    		if (context.getFilterCheckboxes()[x]) {
	    	    			Log.w(TAG, "adding " + context.getFilterTypes()[x]);
	    	    			filterTypes.add(context.getFilterTypes()[x]);
	    	    		}
	    	    	}
	        		mLoader = mObjects.queryLaterObjects(getActivity(), mFeedUri, totalCount, filterTypes.toArray(new String[filterTypes.size()]));
	    		}
        	}
        	else {
        		mLoader = mObjects.queryLaterObjects(getActivity(), mFeedUri, totalCount, null);
        	}
    	}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}

	private OnItemClickListener mItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            DbObject.ItemClickListener.getInstance(getActivity()).onClick(view);
        }
	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
            DbObject.ItemLongClickListener.getInstance(getActivity()).onLongClick(view);
            return true;
        }
        
    };

    class AppSpinner extends BaseAdapter {
        final Context mContext;
        final List<MusubiApp> mApps;
        final PackageManager mPackageManager;

        public AppSpinner(Context context) {
            mContext = context;
            mPackageManager = context.getPackageManager();
            mApps = LaunchApplicationAction.getInstalledApps(context);
        }

        @Override
        public int getCount() {
            return mApps.size();
        }

        @Override
        public Object getItem(int position) {
            return mApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if (convertView != null) {
                iv = (ImageView) convertView;
                iv.setLayoutParams(CommonLayouts.WRAPPED);
            } else {
                iv = new ImageView(mContext);
            }
            Drawable icon = mApps.get(position).getAppInfo().loadIcon(mPackageManager);
            iv.setImageDrawable(icon);
            return iv;
        }
    }
}
