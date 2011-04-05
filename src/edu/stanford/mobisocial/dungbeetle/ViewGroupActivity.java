package edu.stanford.mobisocial.dungbeetle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.facebook.FacebookInterfaceActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;


public class ViewGroupActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mgr = new BitmapManager(10);
	private NotificationManager mNotificationManager;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        String group_id = (String) this.getIntent().getExtras().get("group_id");
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members/" + group_id), 
            new String[]{Contact._ID, 
                         Contact.NAME, 
                         Contact.EMAIL, 
                         Contact.PERSON_ID,
                         Contact.PUBLIC_KEY}, 
            null, null, null);
		mContacts = new ContactListCursorAdapter(this, c);
		setListAdapter(mContacts);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
        
		lv.setOnItemClickListener(this);
	}

    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        //if (v.getId()==R.id.list) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        //menu.setHeaderTitle(((Friend)mContacts.get(info.position)).getUserName());
        menu.setHeaderTitle("Menu");
        //String[] menuItems = getResources().getStringArray(R.array.menu);
        //for (int i = 0; i<menuItems.length; i++) {
        menu.add(Menu.NONE, 0, 0, "Manage groups");
        //}
        // }
    }*/
    
    public boolean showGroupPicker(final Contact contact) {
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups_membership/" + contact.personId), 
            new String[]{"_id", "group_id"}, 
            null, null, null);
        Log.i("DBHelper", "person " + contact.personId);
        
        HashMap groupMemberships = new HashMap();
        
        if(c != null && c.moveToFirst()){
        	int group_id_col = c.getColumnIndexOrThrow("group_id");
        	do {
        		groupMemberships.put(c.getString(group_id_col), 1);
        	}while(c.moveToNext());
        }
        
        c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
            new String[]{"_id", "group_id", "feed_name"}, 
            null, null, null);

        
        CharSequence[] groups = new CharSequence[c.getCount()];
        boolean[] tempSelected = new boolean[c.getCount()];
        Log.i("DBHelper", c.getCount() + " groups");
        if(c.moveToFirst()) {
                int group_id_col = c.getColumnIndexOrThrow("group_id");
                int i = 0;
                do{
                    groups[i] = c.getString(group_id_col);
                    if(groupMemberships.containsKey(c.getString(group_id_col)))
                    {
                    	tempSelected[i] = true;
                    }
                    i++; 
                }while(c.moveToNext());
            }
        final CharSequence[] items = groups;
        
        final boolean[] selected = tempSelected;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Groups");
        
        builder.setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
	            public void onClick(DialogInterface dialog, int item, boolean isChecked) {
	            	DBHelper helper = new DBHelper(ViewGroupActivity.this);

            		ContentValues values = new ContentValues();
                     values.put("group_id", (String) items[item]);
                     values.put("person_id", contact.personId);
  	               
                     
	            	if(isChecked) 
	            	{
	                     helper.insertGroupMember(values);
	            	}
	            	else
	            	{
	            		helper.deleteGroupMember(values);
	            	}
	            	
	            }
	        });
        builder.setPositiveButton("Done",
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int whichButton) {}
                                  });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        final Contact c = new Contact((Cursor)mContacts.getItem(position));
        final CharSequence[] items = new CharSequence[]{ "Send Message", "Start Application", "Manage Groups" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actions");
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item){
                    case 0: 
                        sendMessageToContact(c);
                        break;
                    case 1:
                        startApplicationWithContact(c);
                        break;
                    case 2:
                    	showGroupPicker(c);
                    	break;
                    }
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void sendMessageToContact(final Contact contact){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Enter message:");
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    DBHelper helper = new DBHelper(ViewGroupActivity.this);
                    helper.setMyEmail(input.getText().toString());
                    Helpers.sendIM(ViewGroupActivity.this, Collections.singletonList(contact), input.getText().toString());
                }
            });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
        alert.show();
    }

    private void startApplicationWithContact(final Contact contact){
        final PackageManager mgr = getPackageManager();
        Intent i = new Intent("android.intent.action.CONFIGURE");
        i.addCategory("android.intent.category.P2P");
        final List<ResolveInfo> infos = mgr.queryBroadcastReceivers(i, 0);
        if(infos.size() > 0){
            ArrayList<String> names = new ArrayList<String>();
            for(ResolveInfo info : infos){
                names.add(info.loadLabel(mgr).toString());
            }
            final CharSequence[] items = names.toArray(new CharSequence[]{});
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                                        startActivity(launch);
                                        Helpers.sendApplicationInvite(
                                        		ViewGroupActivity.this,
                                            Collections.singletonList(contact), info.packageName, arg);
                                    }
                                    else{
                                        Toast.makeText(getApplicationContext(), 
                                                       "Sorry, no response from applications.",
                                                       Toast.LENGTH_SHORT).show();
                                    }
                                }
                            };
                        sendOrderedBroadcast(i, null, rec, null, RESULT_OK, null, null);
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else{
            Toast.makeText(getApplicationContext(), 
                           "Sorry, couldn't find any compatible apps.", 
                           Toast.LENGTH_SHORT).show();
        }
    }


    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            String name = c.getString(c.getColumnIndexOrThrow(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }

            String email = c.getString(c.getColumnIndexOrThrow(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mgr.lazyLoadImage(icon, Gravatar.gravatarUri(email));            
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndexOrThrow(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }

            String email = c.getString(c.getColumnIndexOrThrow(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mgr.lazyLoadImage(icon, Gravatar.gravatarUri(email));            
        }

    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, 0, 0, "Set email (debug)");
        menu.add(0, 1, 0, "Facebook Bootstrap");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case 0: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter email:");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DBHelper helper = new DBHelper(ViewGroupActivity.this);
                        helper.setMyEmail(input.getText().toString());
                    }
                });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();
            return true;
        }
        case 1: {
            Intent intent = new Intent(this, FacebookInterfaceActivity.class);
            startActivity(intent); 
            return true;
        }
        default: return false;
        }
    }


}



