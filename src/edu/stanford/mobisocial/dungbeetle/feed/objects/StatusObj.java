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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * A text-based status update.
 *
 */
public class StatusObj extends DbEntryHandler implements FeedRenderer, Activator {

    public static final String TYPE = "status";
    public static final String TEXT = "text";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(String status) {
        return new DbObject(TYPE, json(status));
    }

    public static JSONObject json(String status){
        JSONObject obj = new JSONObject();
        try{
            obj.put(TEXT, status);
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
        JSONObject content = obj.getJson();
        TextView valueTV = new TextView(context);
        valueTV.setText(content.optString(TEXT));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        if(Linkify.addLinks(valueTV, Linkify.ALL)) {
            if(!allowInteractions)
            	valueTV.setMovementMethod(null);
        }

        frame.addView(valueTV);
    }

	static final Pattern p = Pattern.compile("\\b[-0-9a-zA-Z+\\.]+:\\S+");
	@Override
    public void activate(Context context, SignedObj obj){
    	//linkify should have picked it up already but if we are in TV mode we
    	//still need to activate
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String text = obj.getJson().optString(TEXT);
        
        //launch the first thing that looks like a link
        Matcher m = p.matcher(text);
        while(m.find()) {
	        Uri uri = Uri.parse(m.group());
	        String scheme = uri.getScheme();
	
	        if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                intent.setData(uri);
	            if (!(context instanceof Activity)) {
	                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            }
	            context.startActivity(intent);
	            return;
	        }
        }    
    }
}
