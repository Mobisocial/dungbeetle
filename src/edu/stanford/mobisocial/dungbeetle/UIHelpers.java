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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class UIHelpers {

    public static final String TAG = "UIHelpers";

    public static void sendIM(final Context context, 
                              final Collection<Contact> contacts){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setMessage("Enter message:");
        final EditText input = new EditText(context);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Helpers.sendIM(
                        context, 
                        contacts,
                        input.getText().toString());
                }
            });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
        alert.show();
    }

    public static void showGroupPicker(final Context context, 
                                       final Contact contact,
                                       final Intent intent) {
        Cursor c;
        HashSet<Long> groupMemberships = null;
        if(contact != null) {
            c = context.getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                          "/groups_membership/" + contact.id), 
                new String[]{GroupMember._ID, GroupMember.GROUP_ID}, 
                null, null, null);
            try {
	            groupMemberships = new HashSet<Long>();
	            if(c.moveToFirst()) do {
	                groupMemberships.add(
	                    c.getLong(c.getColumnIndexOrThrow(GroupMember.GROUP_ID)));
	            } while(c.moveToNext());
            } finally {
            	c.close();
            }
        }
        c = context.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
            null, null, null, null);
        try { 
	        final long[] groupIds = new long[c.getCount()];
	        final Uri[] feedUris = new Uri[c.getCount()];
	        final CharSequence[] groupNames = new CharSequence[c.getCount()];
	        final boolean[] tempSelected = new boolean[c.getCount()];
	        
	        int i = 0;
	        if(c.moveToFirst()) do {
	            groupNames[i] = c.getString(c.getColumnIndexOrThrow(Group.NAME));
	            feedUris[i] = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + c.getString(c.getColumnIndexOrThrow(Group.FEED_NAME)));
	            groupIds[i] = c.getLong(c.getColumnIndexOrThrow(Group._ID));
	            if(contact != null && groupMemberships != null && groupMemberships.contains(
	                   c.getLong(c.getColumnIndexOrThrow(Group._ID)))){
	                tempSelected[i] = true;
	            }
	            i++;
	        } while(c.moveToNext());
	
	        AlertDialog.Builder builder = new AlertDialog.Builder(context);
	        if(contact != null) {
	            builder.setTitle("Member of");
	        }
	        else {
	            builder.setTitle("Choose groups");
	        }
	        builder.setMultiChoiceItems(
	            groupNames, tempSelected, 
	            new DialogInterface.OnMultiChoiceClickListener() {
	                public void onClick(DialogInterface dialog, int item, boolean isChecked) {
	                }
	            });
	
	        builder.setPositiveButton("Close",
	        new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                if(intent != null) {
	                    handleGroupsIntent(context, intent, feedUris, tempSelected);
	//                    context.startActivity(intent);
	                }
	            }
	        });
	        AlertDialog alert = builder.create();
	        alert.show();
        } finally {
        	c.close();
        }
    }

    private static void handleGroupsIntent(
            Context context, Intent intent, Uri[] uris, boolean[] using) {
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action))
        {
            if (extras.containsKey(Intent.EXTRA_TEXT)) {
                Uri uri = Uri.parse(extras.getCharSequence(Intent.EXTRA_TEXT).toString());
                
                DbObject obj = StatusObj.from(uri.toString());
                
                for(int i = 0; i < using.length; i++) {
                    if(using[i]) {                    
                        Helpers.sendToFeed(context, obj, uris[i]);
                    }
                }
            }
            if (extras.containsKey(Intent.EXTRA_STREAM))
            {
                try
                {
                    // Get resource path from intent callee
                    Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

                    // Query gallery for camera picture via
                    // Android ContentResolver interface
                    ContentResolver cr = context.getContentResolver();
                    InputStream is = cr.openInputStream(uri);
                    // Get binary bytes for encode
                    byte[] data = getBytesFromFile(is);


                    BitmapFactory.Options options = new BitmapFactory.Options();
			        options.inSampleSize = 8;
			        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(
			                data, 0, data.length, options);

			        // Bitmap sourceBitmap = Media.getBitmap(getContentResolver(),
			        // Uri.fromFile(file) );
			        int width = sourceBitmap.getWidth();
			        int height = sourceBitmap.getHeight();
			        int cropSize = Math.min(width, height);

			        int targetSize = 200;
			        float scaleSize = ((float) targetSize) / cropSize;

			        Matrix matrix = new Matrix();
			        matrix.postScale(scaleSize, scaleSize);
			        float rotation = PhotoTaker.rotationForImage(context, uri);
			        if (rotation != 0f) {
	                    matrix.preRotate(rotation);
			        }

                    Bitmap resizedBitmap = Bitmap.createBitmap(
                            sourceBitmap, 0, 0, width, height, matrix, true);
                    

			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			        data = baos.toByteArray();
			        sourceBitmap.recycle();
			        sourceBitmap = null;
			        resizedBitmap.recycle();
			        resizedBitmap = null;
			        System.gc();
                    DbObject obj = PictureObj.from(data);
                    
                    for(int i = 0; i < using.length; i++) {
                        if(using[i]) {                    
                            Helpers.sendToFeed(context, obj, uris[i]);
                        }
                    }
                } catch (Exception e)
                {
                    Log.e(context.getClass().getName(), e.toString());
                }

            }
        }
    }

    
    private static byte[] getBytesFromFile(InputStream is)
    {
	    try
	    {
		    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		    int nRead;
		    byte[] data = new byte[16384];

		    while ((nRead = is.read(data, 0, data.length)) != -1)
		    {
			    buffer.write(data, 0, nRead);
		    }

		    buffer.flush();

		    return buffer.toByteArray();
	    } catch (IOException e)
	    {
		    Log.e("com.eggie5.post_to_eggie5", e.toString());
		    return null;
	    }
    }
}
