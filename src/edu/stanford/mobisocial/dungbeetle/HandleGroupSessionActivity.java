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

package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;


public class HandleGroupSessionActivity extends Activity {
	private TextView mNameText;
    public static final String TAG = "HandleGroupSessionActivity";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_group);
		Intent intent = getIntent();
		final String scheme=intent.getScheme();
		if (!HomeActivity.GROUP_SESSION_SCHEME.equals(scheme)) {
		    Toast.makeText(this, "Failed to receive url :(", Toast.LENGTH_SHORT).show();
		    return;
		}

		final Uri uri = intent.getData();
		if(uri != null) {
            GroupProviders.GroupProvider gp1 = GroupProviders.forUri(uri);
            String feedName = gp1.feedName(uri);
            DBHelper helper = DBHelper.getGlobal(this);
            Maybe<Group> mg = helper.groupByFeedName(feedName);
            long id = -1;
            try {
                // group exists already, load view
                Group g = mg.get();
                id = g.id;

                Group.view(HandleGroupSessionActivity.this, g);
                finish();
            } catch(Maybe.NoValError e) {
                // group does not exist yet, time to prompt for join
                Log.i(TAG, "Read uri: " + uri);
                GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
                String groupName = gp.groupName(uri);
                mNameText = (TextView)findViewById(R.id.text);
                mNameText.setText("Would you like to join the group '" + groupName + "'?");
                Button b1 = (Button)findViewById(R.id.yes_button);
                b1.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Group.join(HandleGroupSessionActivity.this, uri);
                            finish();
                        }
                    });
                Button b2 = (Button)findViewById(R.id.no_button);
                b2.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            finish();
                        }
                    });
            }
		} else {
			Toast.makeText(this, "Received null url...", Toast.LENGTH_SHORT).show();
		}
	}
}