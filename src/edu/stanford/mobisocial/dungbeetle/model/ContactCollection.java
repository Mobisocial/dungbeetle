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

package edu.stanford.mobisocial.dungbeetle.model;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Lazy iterator of contacts in a group.
 */
public class ContactCollection extends AbstractCollection<Contact> {
	//This used to lazy load through an iterator, but the problem with
	//that is that it isn't possible to know when to clean up
	//the cursor!
	LinkedList<Contact> contacts_ = new LinkedList<Contact>();

    public ContactCollection(long groupId, DBHelper helper){
        Cursor cursor = helper.queryGroupContacts(groupId);
        try {
	        if(cursor.moveToFirst()) do {
	        	contacts_.add(new Contact(cursor));
	        } while(cursor.moveToNext());
        } finally {
        	cursor.close();
        }
    }

    @Override
    public int size(){
        return contacts_.size();
    }

    @Override
    public Iterator<Contact> iterator(){
    	return contacts_.iterator();
    }
}
