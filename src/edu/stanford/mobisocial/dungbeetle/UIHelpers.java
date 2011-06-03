package edu.stanford.mobisocial.dungbeetle;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;

import java.util.Collection;
import java.util.HashSet;

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
                                       final Contact contact) {
        Cursor c = context.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                      "/groups_membership/" + contact.id), 
            new String[]{GroupMember._ID, GroupMember.GROUP_ID}, 
            null, null, null);
        HashSet<Long> groupMemberships = new HashSet<Long>();
        c.moveToFirst();
        while(!c.isAfterLast()){
            groupMemberships.add(
                c.getLong(c.getColumnIndexOrThrow(GroupMember.GROUP_ID)));
            c.moveToNext();
        }

        c = context.getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
            null, null, null, null);
        final long[] groupIds = new long[c.getCount()];
        final CharSequence[] groupNames = new CharSequence[c.getCount()];
        final boolean[] tempSelected = new boolean[c.getCount()];
        c.moveToFirst();
        int i = 0;
        while(!c.isAfterLast()){
            groupNames[i] = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            groupIds[i] = c.getLong(c.getColumnIndexOrThrow(Group._ID));
            if(groupMemberships.contains(
                   c.getLong(c.getColumnIndexOrThrow(Group._ID)))){
                tempSelected[i] = true;
            }
            c.moveToNext();
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pick Groups");
        builder.setMultiChoiceItems(
            groupNames, tempSelected, 
            new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                    Long groupId = groupIds[item];
                    Long contactId = contact.id;
                    if(isChecked) {
                        ContentValues values = new ContentValues();
                        values.put(GroupMember.GROUP_ID, groupId);
                        values.put(GroupMember.CONTACT_ID, contactId);
                        context.getContentResolver().insert(
                            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members"), values);
                    }
                    else {
                        context.getContentResolver().delete(
                            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members"),
                            GroupMember.GROUP_ID + "=? AND " + GroupMember.CONTACT_ID + "=?",
                            new String[]{ String.valueOf(groupId), String.valueOf(contactId)});
                    }
                }
            });

        builder.setPositiveButton("Done",
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(
                                          DialogInterface dialog, 
                                          int whichButton) {}
                                  });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
