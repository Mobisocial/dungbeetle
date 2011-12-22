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

import java.math.BigInteger;
import java.security.SecureRandom;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class SharedSecretObj extends DbEntryHandler {

    public static final String TYPE = "shared_secret";
    public static final String RAW = "raw";
    public static final SecureRandom random = new SecureRandom();

    public static byte[] getOrPushSecret(Context context, Contact other) {
    	if(other.secret != null) {
    		return other.secret;
    	}
    	//TODO: this really needs to be refactored into the contentprovider/helpers etc
        ContentValues values = new ContentValues();
        byte[] ss = new byte[32];
        random.nextBytes(ss);
        values.put(Contact.SHARED_SECRET, ss);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{String.valueOf(other.id)});
        Helpers.sendMessage(context, other.id, json(ss), TYPE);
        return ss;
    }

    public static JSONObject json(byte[] shared_secret){
        JSONObject obj = new JSONObject();
        try{
            obj.put(RAW, FastBase64.encodeToString(shared_secret));
        }catch(JSONException e){}
        return obj;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void handleDirectMessage(Context context, Contact from, JSONObject obj){
        String raw_b64;
		try {
			raw_b64 = obj.getString(RAW);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
        byte[] ss = FastBase64.decode(raw_b64);
        if(from.secret != null && new BigInteger(from.secret).compareTo(new BigInteger(ss)) > 0) {
        	//ignore the new key according to a time independent metric...
        	return;
        }

        ContentValues values = new ContentValues();
        values.put(Contact.SHARED_SECRET, ss);
        context.getContentResolver().update(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            values, "_id=?", new String[]{String.valueOf(from.id)});
    }
    
}
