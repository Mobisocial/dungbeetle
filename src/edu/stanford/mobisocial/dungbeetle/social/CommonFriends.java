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

package edu.stanford.mobisocial.dungbeetle.social;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.skjegstad.utils.BloomFilter;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class CommonFriends {
    public static BloomFilter getFriendsBloomFilter(final Context c) {
        BloomFilter<String> friendsFilter = new BloomFilter<String>(.001, 1000);
        Cursor cursor = c.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            new String[]{Contact.PUBLIC_KEY}, 
            null, 
            null, 
            null);
        try {
        	if(cursor.moveToFirst()) do {
	            String publicKey = cursor.getString(cursor.getColumnIndexOrThrow(Contact.PUBLIC_KEY));
	            friendsFilter.add(publicKey);
	        } while(cursor.moveToNext());
        } finally {
        	cursor.close();
        }
        return friendsFilter;    
    }

    public static Contact[] checkFriends(final Context c, BloomFilter friendsFilter) {
        ArrayList<Contact> friends = new ArrayList<Contact>();
        Cursor cursor = c.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
            null, 
            null, 
            null, 
            null);
        try {
        	if(cursor.moveToFirst()) do {
	            Contact contact = new Contact(cursor);
	            String publicKey = cursor.getString(cursor.getColumnIndexOrThrow(Contact.PUBLIC_KEY));
	            String name = cursor.getString(cursor.getColumnIndexOrThrow(Contact.NAME));
	            if(friendsFilter.contains(publicKey)) {
	                Log.w("bloomfilter", name + " is a friend");
	                friends.add(contact);
	            }
	            else {
	                Log.w("bloomfilter", name + " is not a friend");
	            }
        	} while(cursor.moveToNext());
        } finally {
        	cursor.close();
        }

        Contact[] friendsArray = new Contact[friends.size()];
        return friends.toArray(friendsArray);
    }
}
