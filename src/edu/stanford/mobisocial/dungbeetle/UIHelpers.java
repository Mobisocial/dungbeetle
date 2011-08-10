package edu.stanford.mobisocial.dungbeetle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import android.content.Intent;

import java.util.Collection;
import java.util.HashSet;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.Helpers;

import android.content.ContentResolver;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

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
            groupMemberships = new HashSet<Long>();
            c.moveToFirst();
            while(!c.isAfterLast()){
                groupMemberships.add(
                    c.getLong(c.getColumnIndexOrThrow(GroupMember.GROUP_ID)));
                c.moveToNext();
            }
        }
        c = context.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
            null, null, null, null);
        final long[] groupIds = new long[c.getCount()];
        final Uri[] feedUris = new Uri[c.getCount()];
        final CharSequence[] groupNames = new CharSequence[c.getCount()];
        final boolean[] tempSelected = new boolean[c.getCount()];
        c.moveToFirst();
        int i = 0;
        while(!c.isAfterLast()){
            groupNames[i] = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            feedUris[i] = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + c.getString(c.getColumnIndexOrThrow(Group.FEED_NAME)));
            groupIds[i] = c.getLong(c.getColumnIndexOrThrow(Group._ID));
            if(contact != null && groupMemberships != null && groupMemberships.contains(
                   c.getLong(c.getColumnIndexOrThrow(Group._ID)))){
                tempSelected[i] = true;
            }
            c.moveToNext();
            i++;
        }

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
    }

    private static void handleGroupsIntent(
            Context context, Intent intent, Uri[] uris, boolean[] using) {
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action))
        {
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
			        Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 0, 0, cropSize, cropSize);

			        int targetSize = 200;
			        float scaleSize = ((float) targetSize) / cropSize;

			        Matrix matrix = new Matrix();
			        matrix.postScale(scaleSize, scaleSize);
			        if (uri.getScheme().equals("content")) {
			            String[] projection = { Images.ImageColumns.ORIENTATION };
			            Cursor c = context.getContentResolver().query(
			                    uri, projection, null, null, null);
			            if (c.moveToFirst()) {
			                int rotation = c.getInt(0);
			                if (rotation != 0f) {
			                    matrix.preRotate(rotation);
			                }
			            }
			        } else if (uri.getScheme().equals("file")) {
			            try {
	                        ExifInterface exif = new ExifInterface(uri.getPath());
	                        int rotation = (int) PhotoTaker.exifOrientationToDegrees(
	                                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
	                                        ExifInterface.ORIENTATION_NORMAL));
	                        matrix.preRotate(rotation);
	                    } catch (IOException e) {
	                        Log.e(TAG, "Error checking exif", e);
	                    }
			        }
                    Bitmap resizedBitmap;
                    
                    resizedBitmap = Bitmap.createBitmap(
                            sourceBitmap, 0, 0, width, height, matrix, true);
                    

			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			        data = baos.toByteArray();


                    
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
