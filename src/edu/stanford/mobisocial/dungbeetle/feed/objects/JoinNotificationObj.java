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

import java.util.Collection;

import mobisocial.socialkit.Obj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;

public class JoinNotificationObj extends DbEntryHandler implements UnprocessedMessageHandler, FeedRenderer {
    private static final String TAG = "dbJoin";
    private static boolean DBG = false;
    public static final String TYPE = "join_notification";
    public static final String URI = "uri";

    @Override
    public String getType() {
        return TYPE;
    }


    public static DbObject from(String uri) {
        return new DbObject(TYPE, json(uri));
    }
    
    public static JSONObject json(String uri){
        JSONObject obj = new JSONObject();
        try{
            obj.put(URI, uri);
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public void handleDirectMessage(final Context context, Contact from, JSONObject obj) {
    }

    @Override
    public Pair<JSONObject, byte[]> handleUnprocessed(final Context context, JSONObject obj) {
        if (DBG) Log.i(TAG, "Message to update group. ");
        String feedName = obj.optString("feedName");
        final Uri uri = Uri.parse(obj.optString(JoinNotificationObj.URI));
        final GroupProviders.GroupProvider h = GroupProviders.forUri(uri);
        final DBHelper helper = DBHelper.getGlobal(context);
        final IdentityProvider ident = new DBIdentityProvider(helper);
        Maybe<Group> mg = helper.groupByFeedName(feedName);
        try {
            // group exists already, load view
            final Group g = mg.get();

            GroupProviders.runBackgroundGroupTask(g.id, new Runnable(){
                public void run(){
                	Collection<Contact> existingContacts = g.contactCollection(helper);
                	
                    h.handle(g.id, uri, context, g.version, false);
                    
	                Collection<Contact> newContacts = g.contactCollection(helper);
	                newContacts.removeAll(existingContacts);
                    Helpers.resendProfile(context, newContacts, true);
                }
            });
        }
        catch(Maybe.NoValError e) { }
        ident.close();
        
        helper.close();
        return null;
    }


    @Override
    public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
        TextView valueTV = new TextView(context);
        valueTV.setText("I'm here!");
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }
}
