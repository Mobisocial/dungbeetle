package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class UIHelpers {

    public static void sendMessageToContact(final Context context, 
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

    public static void startApplicationWithContact(final Context context, 
                                                   final Collection<Contact> contacts){
        final PackageManager mgr = context.getPackageManager();
        Intent i = new Intent("android.intent.action.CONFIGURE");
        i.addCategory("android.intent.category.P2P");
        final List<ResolveInfo> infos = mgr.queryBroadcastReceivers(i, 0);
        if(infos.size() > 0){
            ArrayList<String> names = new ArrayList<String>();
            for(ResolveInfo info : infos){
                names.add(info.loadLabel(mgr).toString());
            }
            final CharSequence[] items = names.toArray(new CharSequence[]{});
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Share application:");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        final ResolveInfo info = infos.get(item);
                        Intent i = new Intent();
                        i.setClassName(info.activityInfo.packageName, 
                                       info.activityInfo.name);
                        i.setAction("android.intent.action.CONFIGURE");
                        i.addCategory("android.intent.category.P2P");
                        BroadcastReceiver rec = new BroadcastReceiver(){
                                public void onReceive(Context c, Intent i){
                                    Intent launch = new Intent();
                                    launch.setAction(Intent.ACTION_MAIN);
                                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                                    launch.setPackage(info.activityInfo.packageName);
                                    List<ResolveInfo> resolved = 
                                        mgr.queryIntentActivities(launch, 0);
                                    if (resolved.size() > 0) {
                                        ActivityInfo info = resolved.get(0).activityInfo;
                                        String arg = getResultData();
                                        launch.setComponent(new ComponentName(
                                                                info.packageName,
                                                                info.name));
                                        launch.putExtra("creator", true);
                                        launch.putExtra(
                                            "android.intent.extra.APPLICATION_ARGUMENT",
                                            arg);
                                        context.startActivity(launch);
                                        Helpers.sendApplicationInvite(
                                            context,
                                            contacts, 
                                            info.packageName, arg);
                                    }
                                    else{
                                        Toast.makeText(context, 
                                                       "Sorry, no response from applications.",
                                                       Toast.LENGTH_SHORT).show();
                                    }
                                }
                            };
                        context.sendOrderedBroadcast(i, null, rec, null, Activity.RESULT_OK, null, null);
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else{
            Toast.makeText(context.getApplicationContext(), 
                           "Sorry, couldn't find any compatible apps.", 
                           Toast.LENGTH_SHORT).show();
        }
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
