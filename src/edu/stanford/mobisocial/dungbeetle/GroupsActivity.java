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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mobisocial.nfc.NdefFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;

/**
 * UI for showing a list of all user groups.
 */
public class GroupsActivity extends ListActivity implements OnItemClickListener {
	private GroupListCursorAdapter mGroups;
    public static final String SHARE_SCHEME = "db-share-contact";
	private static final int REQUEST_INVITE_TO_GROUP = 1;
	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();
    public final String TAG = "GroupsActivity";

    /*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
        if(Build.VERSION.SDK_INT < 11)
        	intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	else 
    		intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (getTitle ());
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groups);
        setTitleFromActivityLabel (R.id.title_text);
        
        mHelper = DBHelper.getGlobal(this);
        String selection = Group.GROUP_TYPE + " = ? AND " + Group.FEED_NAME + " not in " +
                "(select " + DbObject.CHILD_FEED_NAME + " from " + DbObject.TABLE +
                " where " + DbObject.CHILD_FEED_NAME + " is not null)";
        String[] selectionArgs = new String[] { Group.TYPE_GROUP };
        //actually what ultimately gets called is DBHelper.queryGroups() so change stuff there
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"),
            null, selection, selectionArgs, null);
		mGroups = new GroupListCursorAdapter(this, c);
		setListAdapter(mGroups);
		getListView().setOnItemClickListener(this);
		registerForContextMenu(getListView());
        //in case there was an FC, we must restart the service whenever one of our dialogs is opened.
        startService(new Intent(this, DungBeetleService.class));
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor)mGroups.getItem(info.position);
        final Group g = new Group(cursor);
        menu.setHeaderTitle(g.name);
        String[] menuItems = new String[]{ "Delete"};//, "Write to Tag" };
        for (int i = 0; i< menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        Cursor cursor = (Cursor)mGroups.getItem(info.position);
        final Group g = new Group(cursor);
        switch(menuItemIndex) {
        case 0:
            mHelper.deleteGroup(GroupsActivity.this, g.id);
            break;
        case 1:
            if(g.dynUpdateUri != null){
                Uri uri = Uri.parse(g.dynUpdateUri);
                ((HomeActivity)getParent()).writeGroupToTag(uri);
            }
            else{
				Toast.makeText(this, "Invalid group.", Toast.LENGTH_SHORT).show();
            }
            break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
        Cursor cursor = (Cursor)mGroups.getItem(position);
        final Group g = new Group(cursor);
        Group.view(this, g);
    }

    private class GroupListCursorAdapter extends CursorAdapter {
        public GroupListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.groups_item, parent, false);
            bindView(v, context, c);
            return v;
        }

        public MyLocation myLocation;
        public MyLocation.LocationResult locationResult;

        private void locationClick() {
            myLocation.getLocation(GroupsActivity.this, locationResult);
        }
        
        @Override
        public void bindView(View v, Context context, Cursor c) {

            final Group g = new Group(c);
            final Collection<Contact> contactsInGroup = g.contactCollection(mHelper);
        
            String name = c.getString(c.getColumnIndexOrThrow(Group.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }           

            ImageView more = (ImageView) v.findViewById(R.id.more);

            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mGroup = Maybe.definitely(g);
                    
                        final ActionItem sendIM = new ActionItem();
                        sendIM.setTitle("Send IM");
                        sendIM.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.sendIM(
                                        GroupsActivity.this, 
                                        contactsInGroup);
                                }
                            });
                    
                        /*final ActionItem startApp = new ActionItem();
                        startApp.setTitle("Start App");
                        startApp.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AppReferenceObj.promptForApplication(GroupsActivity.this, new AppReferenceObj.Callback() {
                                        @Override
                                        public void onAppSelected(String packageName, String arg, Intent localLaunch) {
                                            Helpers.sendMessage(GroupsActivity.this, contactsInGroup,
                                                    new DbObject(InviteToSharedAppFeedObj.TYPE,
                                                            AppReferenceObj.json(packageName, arg)));
                                            GroupsActivity.this.startActivity(localLaunch);
                                        }
                                    });
                                }
                            });*/

                        final ActionItem invite = new ActionItem();
                        invite.setTitle("Invite Contacts");
                        invite.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent i = new Intent();
                                    i.setAction(PickContactsActivity.INTENT_ACTION_PICK_CONTACTS);
                                    i.putExtra(PickContactsActivity.INTENT_EXTRA_NFC_SHARE, ndefForGroup(g));
                                    GroupsActivity.this.startActivityForResult(
                                        i, REQUEST_INVITE_TO_GROUP);   
                                }
                            });

                        final ActionItem nearby = new ActionItem();
                        nearby.setTitle("Broadcast Nearby");
                        nearby.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                
                                    final CharSequence[] items = {"5 minutes", "15 minutes", "1 hour", " 24 hours"};

                                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
                                    builder.setTitle("Choose duration of broadcast");
                                    builder.setItems(items, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, final int item) {
                                            
                                            myLocation = new MyLocation();
                                            locationResult = new MyLocation.LocationResult(){

                                            
                                                final ProgressDialog dialog = ProgressDialog.show(GroupsActivity.this, "", 
                                                            "Preparing broadcast...", true);  
                                                @Override
                                                public void gotLocation(final Location location){
                                                    //Got the location!
                                                    try {
                                                        int minutes;
                                                        if(item == 0) {
                                                            minutes = 5;
                                                        }
                                                        else if(item == 1) {
                                                            minutes = 15;
                                                        }
                                                        else if(item == 2) {
                                                            minutes = 60;
                                                        }
                                                        else if(item == 3) {
                                                            minutes = 1440;
                                                        }
                                                        else
                                                        {
                                                            minutes = 5;
                                                        }
                                                        Uri.Builder b = new Uri.Builder();
                                                        b.scheme("http");
                                                        b.authority("suif.stanford.edu");
                                                        b.path("dungbeetle/nearby.php");
                                                        Uri uri = b.build();
                                                        
                                                        StringBuffer sb = new StringBuffer();
                                                        DefaultHttpClient client = new DefaultHttpClient();
                                                        HttpPost httpPost = new HttpPost(uri.toString());

                                                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                                                    
                                                        nameValuePairs.add(new BasicNameValuePair("group_name", g.name));
                                                        nameValuePairs.add(new BasicNameValuePair("feed_uri", g.dynUpdateUri));
                                                        nameValuePairs.add(new BasicNameValuePair("length", Integer.toString(minutes)));
                                                        nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
                                                        nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(location.getLongitude())));
                                                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                                        try {
                                                            HttpResponse execute = client.execute(httpPost);
                                                            InputStream content = execute.getEntity().getContent();
                                                            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                                                            String s = "";
                                                            while ((s = buffer.readLine()) != null) {
                                                                sb.append(s);
                                                            }
                                                        }
                                                        catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                        String response = sb.toString();
                                                        if(response.equals("1"))
                                                        {
                                                            Toast.makeText(getApplicationContext(), 
                                                                "Now broadcasting for " + items[item], 
                                                                Toast.LENGTH_SHORT).show();
                                                        }    

                                                        Log.i(TAG, response);
                                                    }
                                                    catch(Exception e) {
                                                    }

                                                    
                                                    dialog.dismiss();
                                                }
                                            };

                                            locationClick();
                                        }
                                    });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                    
                                    Log.w(TAG, "name: " + g.name);
                                    
                                    Log.w(TAG, "uri: " + g.dynUpdateUri);
                                    /*Intent i = new Intent();
                                    i.setAction(PickContactsActivity.INTENT_ACTION_PICK_CONTACTS);
                                    i.putExtra(PickContactsActivity.INTENT_EXTRA_NFC_SHARE, ndefForGroup(g));
                                    GroupsActivity.this.startActivityForResult(
                                        i, REQUEST_INVITE_TO_GROUP);   */
                                }
                            });

                    
                        QuickAction qa = new QuickAction(v);

                        //qa.addActionItem(sendIM);
                        //qa.addActionItem(startApp);
                        qa.addActionItem(invite);
                        qa.addActionItem(nearby);
                        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

                        qa.show();
                    }
                });
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INVITE_TO_GROUP) {
            if (resultCode == RESULT_OK) {
                long[] contactIds = data.getLongArrayExtra("contacts");
                try{
                    Helpers.sendGroupInvite(this, contactIds, mGroup.get());
                }catch(Maybe.NoValError e){}
            }
        }
        mGroup = Maybe.unknown();
    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        mHelper.close();
    }

    private NdefMessage ndefForGroup(Group g) {
        return NdefFactory.fromUri(g.dynUpdateUri);
    }
}
