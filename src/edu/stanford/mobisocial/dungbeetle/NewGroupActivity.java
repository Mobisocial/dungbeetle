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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class NewGroupActivity extends Activity {

    
    private DBHelper mHelper;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_group);
        mHelper = DBHelper.getGlobal(this);

		((Button)findViewById(R.id.newGroupOk)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String groupName = ((EditText)findViewById(R.id.newGroupName)).getText().toString();
                Group g;
                if(groupName.length() > 0) {
                    g = Group.create(NewGroupActivity.this, groupName, mHelper);
                } else {
                    g = Group.create(NewGroupActivity.this);
                }

                Uri feedUri = Feed.uriForName(g.feedName);
                Helpers.sendToFeed(NewGroupActivity.this,
                        StatusObj.from("Welcome to " + g.name + "!"), feedUri);
                Feed.view(NewGroupActivity.this, feedUri);
                NewGroupActivity.this.finish();
            }
        });
        ((Button)findViewById(R.id.newGroupCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NewGroupActivity.this.finish();
            }
        });
        //in case there was an FC, we must restart the service whenever one of our dialogs is opened.
        startService(new Intent(this, DungBeetleService.class));
    }
}
