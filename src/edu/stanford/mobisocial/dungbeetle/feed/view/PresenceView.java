/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Presence;

public class PresenceView implements FeedView {
    private final Fragment mFragment;

    public PresenceView() {
        mFragment = new FeedPresenceFragment();
    }
    @Override
    public String getName() {
        return "Sharing";
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    public static class FeedPresenceFragment extends FeedBaseListFragment {
        @Override
        public void onFeedUpdated() {

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            PresenceAdapter adapter = new PresenceAdapter(getActivity(), mFeedUri);
            setListAdapter(adapter);
            getListView().setOnItemClickListener(adapter);
        }
    }

    static class PresenceAdapter extends ArrayAdapter<FeedPresence> 
            implements OnItemClickListener, OnCheckedChangeListener {
        private final Uri mFeedUri;
        private final Context mContext;

        public PresenceAdapter(Activity context, Uri feedUri) {
            super(context, R.layout.widget_selectable_row, R.id.name_text);
            mFeedUri = feedUri;
            mContext = context;
            for (FeedPresence p : Presence.getActivePresenceTypes()) {
                add(p);
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
            FeedPresence p = getItem(position);
            CheckBox checkbox = (CheckBox)v.findViewById(R.id.checkbox);
            checkbox.setChecked(p.isPresent(mFeedUri));
            checkbox.setOnCheckedChangeListener(this);
            return v;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Integer position = (Integer)((View)buttonView.getParent()).getTag();
            getItem(position).setFeedPresence(mContext, mFeedUri, isChecked);
        }
    }
}
