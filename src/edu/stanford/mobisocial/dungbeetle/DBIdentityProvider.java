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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.musubi.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.BitmapFactory;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.JSON;

public class DBIdentityProvider implements IdentityProvider {

    public static final String TAG = "DBIdentityProvider";
	private final RSAPublicKey mPubKey;
	private final String mPubKeyTag;
	private final RSAPrivateKey mPrivKey;
    private final String mEmail;
    private final String mName;
    private final DBHelper mHelper;

    private final String mPubKeyString;

    private Exception mUnclosedException;
    @Override
    protected void finalize() throws Throwable {
    	if(mUnclosedException != null) {
    		throw mUnclosedException;
    	}
        super.finalize();
    }

	public DBIdentityProvider(DBHelper helper) {
		mHelper = helper;
		helper.addRef();
		mUnclosedException = new Exception("Finalized without close being called. Created at...");
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		try {
			if(!c.moveToFirst()) {
	            throw new IllegalStateException("Missing my_info entry!");
	        }
	
	        mPubKeyString = c.getString(c.getColumnIndexOrThrow(MyInfo.PUBLIC_KEY));
	        mPubKey = RSACrypto.publicKeyFromString(mPubKeyString);
	        mPrivKey = RSACrypto.privateKeyFromString(
	                c.getString(c.getColumnIndexOrThrow(MyInfo.PRIVATE_KEY)));
	        mName = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
	        mEmail = c.getString(c.getColumnIndexOrThrow(MyInfo.EMAIL));
	        mPubKeyTag = personIdForPublicKey(mPubKey);
	
	        Log.d(TAG, c.getCount() + " public keys");
		} finally {
			c.close();
		}
    }

	public String userName() {
		return mName;
    }

	public String userEmail() {
	    return mEmail;
    }

    public String userProfile() {
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});

		try {
			c.moveToFirst();
			JSONObject obj = new JSONObject();
	        try {
	            obj.put("name", c.getString(c.getColumnIndexOrThrow(MyInfo.NAME)));
	        } catch(JSONException e) { }
	        return JSON.fastAddBase64(obj.toString(), "picture", c.getBlob(c.getColumnIndexOrThrow(MyInfo.PICTURE)));
		} finally {
	        c.close();
		}
    }

    public String userPublicKeyString() {
        return mPubKeyString;
    }

	public RSAPublicKey userPublicKey(){
        return mPubKey;
    }

	public RSAPrivateKey userPrivateKey(){
        return mPrivKey;
    }

	public String userPersonId(){
        return mPubKeyTag;
    }

	public Contact contactForUser(){
		Cursor c = mHelper.getReadableDatabase().rawQuery("SELECT * FROM " + MyInfo.TABLE, new String[] {});
		try {
			c.moveToFirst();
	        long id = Contact.MY_ID;
	        String name = c.getString(c.getColumnIndexOrThrow(MyInfo.NAME));
	        String email = c.getString(c.getColumnIndexOrThrow(MyInfo.EMAIL));
	        String about = c.getString(c.getColumnIndexOrThrow(MyInfo.ABOUT));
	        //hack, make about info the status field of the contact class
	        Contact contact =  new Contact(id, mPubKeyTag, name, email, 0, 0, false, null, about, null, null, 0);
	        byte[] picdata = c.getBlob(c.getColumnIndexOrThrow(MyInfo.PICTURE)); 
	        if(picdata != null) {
	        	contact.picture = BitmapFactory.decodeByteArray(picdata, 0, picdata.length);
	        }
	        return contact;
		} finally { 
			c.close();
		}
    }

	public RSAPublicKey publicKeyForPersonId(String id){
		if(id.equals(mPubKeyTag)) {
			return mPubKey;
		}
        Cursor c = mHelper.getReadableDatabase().query(Contact.TABLE, new String[]{Contact.PUBLIC_KEY},
            Contact.PERSON_ID + " = ?", new String[]{id}, null, null, null);
        try {
	        if(!c.moveToFirst()) {
	            return null;
	        }
	
	        RSAPublicKey k = RSACrypto.publicKeyFromString(
	                c.getString(c.getColumnIndexOrThrow(Contact.PUBLIC_KEY)));
	        return k;
        } finally {
        	c.close();
        }
    }

	public List<RSAPublicKey> publicKeysForContactIds(List<Long> ids){
        ArrayList<RSAPublicKey> result = new ArrayList<RSAPublicKey>(ids.size());
        SQLiteStatement s = mHelper.getReadableDatabase().compileStatement("SELECT " + Contact.PUBLIC_KEY + " FROM " + Contact.TABLE + " WHERE " + Contact._ID + " = ?");
		for(Long id : ids) {
			s.bindLong(1, id.longValue());
			try {
				String pks = s.simpleQueryForString();
				result.add(RSACrypto.publicKeyFromString(pks));
			} catch (SQLiteDoneException e) {
				Log.e(TAG, "Data consisteny error: unknown contact id " + id);
			}
		}
		s.close();
		return result;
    }

	public static KeyPair generateKeyPair() {
	    try {
	        // Generate a 1024-bit Digital Signature Algorithm (RSA) key pair
	        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	        keyGen.initialize(1024);
	        return keyGen.genKeyPair();
	    } catch (java.security.NoSuchAlgorithmException e) {
	        throw new IllegalStateException("Failed to generate key pair! " + e);
	    }
	}

    public static String publicKeyToString(PublicKey pubkey){
        return FastBase64.encodeToString(pubkey.getEncoded());
    }

    @Override
    public void close() {
    	mUnclosedException = null;
    	mHelper.close();
    }

    @Override
    public String personIdForPublicKey(RSAPublicKey key) {
        return RSACrypto.makePersonIdForPublicKey(key);
    }
}
