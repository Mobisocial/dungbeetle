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

package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import mobisocial.socialkit.musubi.DbObj;
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
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

/**
 * A side-scrolling view of a feed's objects.
 */
public class FeedHistoryFragment extends Fragment implements OnItemClickListener {
    private static final String TAG = "dbobjPager";

    private Uri mFeedUri;
    private SpinnerAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedUri = activity.getIntent().getData();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
            DbObject.bindView(v, context, c, true);
        }
    }

    private SpinnerAdapter getListAdapter(Context context, Uri feedUri) {
        Cursor c = context.getContentResolver().query(feedUri, null,
                DbObjects.getFeedObjectClause(null), null, DbObject._ID + " DESC");
        return new ObjectListCursorAdapter(context, c);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor)mAdapter.getItem(position);
        DbObj obj = App.instance().getMusubi().objForCursor(c);
        if (HomeActivity.DBG) Log.i(TAG, "Clicked object: " + obj.getJson());

        Activator activator = DbObjects.getActivator(obj.getType());
        if(activator != null){
            activator.activate(getActivity(), obj);
        }
    }
}
