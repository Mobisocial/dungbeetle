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

package edu.stanford.mobisocial.dungbeetle.feed.objects;
import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class FeedRefObj extends DbEntryHandler implements FeedRenderer, Activator {

    public static final String TAG = "FeedObj";

    public static final String TYPE = "feed_ref";
    public static final String FEED_ID = "feed_id";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(Group g) {
        return Feed.forGroup(g);
    }

    public static JSONObject json(Group g){
        JSONObject obj = new JSONObject();
        try{
            obj.put(FEED_ID, g.feedName);
        }catch(JSONException e) { 
            e.printStackTrace();
        }
        return obj;
    }

	public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
	    JSONObject content = obj.getJson();
        byte[] raw = obj.getRaw();

		TextView view = new TextView(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        String feedName = content.optString(FEED_ID);
        view.setText(feedName);
        view.setBackgroundColor(Feed.colorFor(feedName));
        frame.addView(view);
	}

	@Override
	public void activate(Context context, SignedObj obj) {
	    Feed feedRef = new Feed(obj.getJson());
	    Maybe<Group> mg = Group.forFeedName(context, feedRef.id());
	    try {
	        Group g = mg.get();
            Group.view(context, g);
	    } catch (NoValError e) {

        }
    }

    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {
        Toast.makeText(context, "received", 400).show();
    }

}
