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
import java.util.Collection;

import android.content.ContentValues;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;

public class MyInfo {
    public static final String TABLE = "my_info";
    public static final String _ID = "_id";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PRIVATE_KEY = "private_key";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PICTURE = "picture";
	public static final String ABOUT = "about";

    public final String publicKey;
    public final String privateKey;
    public final String email;
    public final String name;
    public final Long id;
    public final byte[] picture;
    public final String about;

    public MyInfo(Cursor c){
        id = c.getLong(c.getColumnIndexOrThrow(_ID));
        name = c.getString(c.getColumnIndexOrThrow(NAME));
        email = c.getString(c.getColumnIndexOrThrow(EMAIL));
        publicKey = c.getString(c.getColumnIndexOrThrow(PUBLIC_KEY));
        privateKey = c.getString(c.getColumnIndexOrThrow(PRIVATE_KEY));
        picture = c.getBlob(c.getColumnIndexOrThrow(PICTURE));
        about = c.getString(c.getColumnIndexOrThrow(ABOUT));
    }

    public Collection<Contact> contactCollection(DBHelper helper){
        return new ContactCollection(id, helper);
    }

    public static void setMyName(DBHelper helper, String name) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.NAME, name);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }

    void setMyEmail(DBHelper helper, String email) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.EMAIL, email);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }

    public static void setMyAbout(DBHelper helper, String about) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.ABOUT, about);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }
    public static void setMyPicture(DBHelper helper, byte[] picture) {
        ContentValues cv = new ContentValues();
        cv.put(MyInfo.PICTURE, picture);
        helper.getWritableDatabase().update(MyInfo.TABLE, cv, null, null);
    }
}
