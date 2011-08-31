package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.Gallery;
import android.widget.SpinnerAdapter;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;

public class ObjHistoryFragment extends Fragment implements OnItemClickListener {
    private static final String TAG = "dbobjPager";

    private Uri mFeedUri;
    private ContactCache mContactCache;
    private SpinnerAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = activity.getIntent().getData();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContactCache = new ContactCache(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAdapter = getListAdapter(getActivity(), mFeedUri);
        Gallery gallery = new Gallery(getActivity());
        gallery.setLayoutParams(CommonLayouts.FULL_SCREEN);
        gallery.setAdapter(mAdapter);
        gallery.setOnItemClickListener(this);
        return gallery;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContactCache.close();
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
            DbObject.bindView(v, context, c, mContactCache);
        }
    }

    private SpinnerAdapter getListAdapter(Context context, Uri feedUri) {
        Cursor c = context.getContentResolver().query(feedUri, null,
                DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC");
        return new ObjectListCursorAdapter(context, c);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mAdapter.getItem(position);
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
}
