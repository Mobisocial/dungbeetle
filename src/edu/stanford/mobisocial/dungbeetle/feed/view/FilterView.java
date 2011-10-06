package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.FeedHomeActivity;

public class FilterView implements FeedView {
    private final Fragment mFragment;
    

    public FilterView() {
        mFragment = new FeedFilterFragment();
    }
    @Override
    public String getName() {
        return "Filter";
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    public static class FeedFilterFragment extends FeedBaseListFragment {
        @Override
        public void onFeedUpdated() {

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            FilterAdapter adapter = new FilterAdapter(getActivity(), mFeedUri);
            setListAdapter(adapter);
            getListView().setOnItemClickListener(adapter);
        }
    }

    static class FilterAdapter extends ArrayAdapter<String> 
            implements OnItemClickListener, OnCheckedChangeListener {
        private final Uri mFeedUri;
        private final FeedHomeActivity mContext;

        public FilterAdapter(Activity context, Uri feedUri) {
            super(context, R.layout.widget_selectable_row, R.id.name_text);
            mFeedUri = feedUri;
            mContext = (FeedHomeActivity)context;
            
	    	
            for(String filter : mContext.filterTypes) {
            	add(filter);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CheckBox checkbox = (CheckBox)view.findViewById(R.id.checkbox);
            checkbox.toggle();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setTag(position);
            String p = getItem(position);
            CheckBox checkbox = (CheckBox)v.findViewById(R.id.checkbox);
            
            checkbox.setChecked(mContext.checked[position]);
            checkbox.setOnCheckedChangeListener(this);
            return v;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Integer position = (Integer)((View)buttonView.getParent()).getTag();
            //getItem(position).setFeedPresence(mContext, mFeedUri, isChecked);
            mContext.checked[position] = isChecked;
            Log.w("filterview", "changed: " + isChecked);
        }
    }
}
