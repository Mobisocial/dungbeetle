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

import java.util.HashMap;
import java.util.Map;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class StatsView extends Activity {
    TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);
        mTextView = ((TextView)findViewById(R.id.stats_text));

        Uri feedUri = getIntent().getData();
        Cursor c = getContentResolver().query(feedUri, null, DbObjects.getFeedObjectClause(null),
                null, DbObject._ID + " DESC");
        try {
	        Map<String, Integer> counts = new HashMap<String, Integer>();
	        int totalSize = c.getCount();
	        c.moveToFirst();
	        for (int i = 1; i < totalSize; i++) {
	            c.moveToNext();
	            DbObject obj = DbObject.fromCursor(c);
	            String type = obj.getType();
	            if (counts.containsKey(type)) {
	                counts.put(type, counts.get(type) + 1);
	            } else {
	                counts.put(type, 1);
	            }
	        }
	        StringBuilder builder = new StringBuilder();
	        for (String type : counts.keySet()) {
	            builder.append(type + ": " + counts.get(type)).append("\n");
	        }
	        mTextView.setText(builder.toString());
        } finally {
        	c.close();
        }
    }
}
