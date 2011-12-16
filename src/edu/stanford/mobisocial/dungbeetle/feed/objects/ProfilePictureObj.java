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
import java.util.LinkedList;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class ProfilePictureObj extends DbEntryHandler {
	public static final String TAG = "ProfilePictureObj";
    public static final String TYPE = "profilepicture";
    public static final String DATA = "data";
    public static final String REPLY = "reply";

    @Override
    public String getType() {
        return TYPE;
    }

    public static JSONObject json(byte[] data, boolean reply){
        String encoded = FastBase64.encodeToString(data);
        JSONObject obj = new JSONObject();
        try{
            obj.put(DATA, encoded);
            obj.put(REPLY, reply);
            
        }catch(JSONException e){}
        return obj;
    }

	public boolean handleObjFromNetwork(Context context, Contact from, JSONObject obj) {
		byte[] data = FastBase64.decode(obj.optString(DATA));
		boolean reply = obj.optBoolean(REPLY);
		
		String id = Long.toString(from.id);
		ContentValues values = new ContentValues();
		values.put(Contact.PICTURE, data);
		context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            values, "_id=?", new String[] { id });
		Helpers.invalidateContacts();

        if(reply) {
        	LinkedList<Contact> contacts = new LinkedList<Contact>();
        	contacts.add(from);
        	Helpers.resendProfile(context, contacts, false);
        }
        return false;
	}

	@Override
	public boolean discardOutboundObj() {
		return true;
	};

	/*public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
	    TextView textView = new TextView(context);
	    textView.setText("New profile picture:");
	    textView.setLayoutParams(CommonLayouts.FULL_WIDTH);
		ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(CommonLayouts.WRAPPED);
        String bytes = content.optString(DATA);
        App.instance().objectImages.lazyLoadImage(bytes.hashCode(), bytes, imageView);
        frame.addView(textView);
        frame.addView(imageView);
	}

	@Override
    public void activate(Context context, JSONObject content, byte[] raw){
        Intent intent = new Intent(context, ImageViewerActivity.class);
        String bytes = content.optString(DATA);
        intent.putExtra("b64Bytes", bytes);
        context.startActivity(intent); 
    }*/

    @Override
    public boolean doNotification(Context context, DbObj obj) {
        return false;
    }
}
