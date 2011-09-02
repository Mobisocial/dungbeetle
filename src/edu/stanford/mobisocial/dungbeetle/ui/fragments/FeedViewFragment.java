package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbActions;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.ObjActions;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

/**
 * Shows a series of posts from a feed.
 */
public class FeedViewFragment extends ListFragment implements OnItemClickListener,
        OnEditorActionListener, TextWatcher, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_FEED_URI = "feed_uri";
    LayoutParams LAYOUT_FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

    private ListAdapter mObjects;
	public static final String TAG = "ObjectsActivity";
    private Uri mFeedUri;
    private ContactCache mContactCache;
    private EditText mStatusText;
    private ImageView mSendTextButton;
    private ImageView mSendObjectButton;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
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
        // int color = Feed.colorFor(feedName, Feed.BACKGROUND_ALPHA);
        // getListView().setCacheColorHint(color);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContactCache = new ContactCache(getActivity());
        getLoaderManager().initLoader(0, null, this);
        setListAdapter(mObjects);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mObjects.getItem(position);
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        if (HomeActivity.DBG) Log.i(TAG, "Clicked object: " + jsonSrc);
        try{
            JSONObject obj = new JSONObject(jsonSrc);
            Activator activator = DbObjects.getActivator(obj);
            if(activator != null){
                activator.activate(getActivity(), obj);
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
            DbObject.bindView(v, context, c, mContactCache);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), mFeedUri, null,
                DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mObjects = new ObjectListCursorAdapter(getActivity(), cursor);
        setListAdapter(mObjects);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    void showMenuForObj(int position) {
        Cursor c = (Cursor)mObjects.getItem(position);
        final String type = c.getString(c.getColumnIndexOrThrow(DbObject.TYPE));
        final String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
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
        DialogFragment newFragment = ObjMenuDialogFragment.newInstance(type, json);
        newFragment.show(ft, "dialog");
    }

    public static class ObjMenuDialogFragment extends DialogFragment {
        String mType;
        JSONObject mObj;

        public static ObjMenuDialogFragment newInstance(String type, JSONObject obj) {
            return new ObjMenuDialogFragment(type, obj);
        }

        // Required by framework; fields populated from savedInstanceState.
        public ObjMenuDialogFragment() {
            
        }

        private ObjMenuDialogFragment(String type, JSONObject obj) {
            mType = type;
            mObj = obj;
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
                            actions.get(which).actOn(getActivity(), dbType, mObj);
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
}