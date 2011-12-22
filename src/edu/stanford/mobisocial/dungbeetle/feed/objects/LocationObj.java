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
import java.text.DecimalFormat;
import java.text.NumberFormat;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class LocationObj extends DbEntryHandler implements FeedRenderer, Activator {
    public static final String TYPE = "loc";
    public static final String COORD_LAT = "lat";
    public static final String COORD_LONG = "lon";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject from(Location location) {
        return new DbObject(TYPE, json(location));
    }

    public static JSONObject json(Location location){
        JSONObject obj = new JSONObject();
        try{
            obj.put(COORD_LAT, location.getLatitude());
            obj.put(COORD_LONG, location.getLongitude());
        }catch(JSONException e){}
        return obj;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){

    }

    @Override
    public void render(Context context, ViewGroup frame, Obj obj, boolean allowInteractions) {
        JSONObject content = obj.getJson();
        TextView valueTV = new TextView(context);
        NumberFormat df =  DecimalFormat.getNumberInstance();
        df.setMaximumFractionDigits(5);
        df.setMinimumFractionDigits(5);
        
        String msg = "I'm at " + 
        	df.format(content.optDouble(COORD_LAT)) +
        	", " +
        	df.format(content.optDouble(COORD_LONG));


        valueTV.setText(msg);
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
    }

    @Override
    public void activate(Context context, SignedObj obj) {
        JSONObject content = obj.getJson();
        String loc = "geo:" + content.optDouble(COORD_LAT) + "," +
                content.optDouble(COORD_LONG) + "?z=17";
        Intent map = new Intent(Intent.ACTION_VIEW, Uri.parse(loc));
        context.startActivity(map);
    }

    @Override
    public boolean doNotification(Context context, DbObj obj) {
        return false;
    }
}
