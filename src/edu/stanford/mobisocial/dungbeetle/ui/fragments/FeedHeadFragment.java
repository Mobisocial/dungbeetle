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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

public class FeedHeadFragment extends Fragment {

    public static final String ARG_FEED_URI = "feed_uri";

	public static final String TAG = "ObjectsActivity";
	private ContentObserver mFeedObserver;
	private Uri mFeedUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View tabs = getActivity().findViewById(R.id.tab_frame);
		if (tabs != null) {
		    tabs.setVisibility(View.GONE);
		}
		View view = inflater.inflate(R.layout.objects_item, container, false);
		view.setLayoutParams(CommonLayouts.FULL_WIDTH);
		view.setId(R.id.feed_view);
		return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFeedObserver = new ContentObserver(new Handler(getActivity().getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                bindCurrentView();
            }
        };
        mFeedUri = getArguments().getParcelable(ARG_FEED_URI);
        ContentResolver resolver = getActivity().getContentResolver();
        resolver.registerContentObserver(mFeedUri, true, mFeedObserver);
        bindCurrentView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getContentResolver().unregisterContentObserver(mFeedObserver);
    }

    public static String getFeedObjectClause() {
        String[] types = DbObjects.getRenderableTypes();
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }

    private void bindCurrentView() {
        Cursor c = getActivity().getContentResolver().query(mFeedUri, null, getFeedObjectClause(),
                null, DbObject._ID + " DESC");
        try {
	        if (c.moveToFirst()) {
	            View v = getActivity().findViewById(R.id.feed_view);
	        }
        } finally {
        	c.close();
        }
    }
}