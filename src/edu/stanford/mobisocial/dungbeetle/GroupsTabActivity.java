package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.widget.TabHost;
import android.widget.Toast;
import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;
import android.widget.TextView;
import android.util.Log;
import android.content.Context;

import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.util.MyLocation;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.location.Location;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import android.content.DialogInterface;


import android.widget.EditText;
/**
 * Represents a group by showing its feed and members.
 * TODO: Accept only a group_id extra and query for other parameters.
 */
public class GroupsTabActivity extends TabActivity
{
    private Nfc mNfc;
    private Uri mExternalFeedUri;
    private Uri mInternalFeedUri;
    private static final int REQUEST_BT_BROADCAST = 2;
    private static final int REQUEST_BT_ENABLE = 3;

    public final String TAG = "GroupsTabActivity";

    /*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, DungBeetleActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId, String title)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (title);
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }


    public void onClickBroadcast(View v) {
        new AlertDialog.Builder(GroupsTabActivity.this)
            .setTitle("Share thread...")
            .setItems(new String[] {"Send to friend", "Broadcast nearby"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            sendToFriend();
                            break;
                        case 1:
                            broadcastNearby();
                            break;
                    }
                }
            }).show();
    }

    public void sendToFriend() {
        new AlertDialog.Builder(GroupsTabActivity.this)
        .setTitle("Share thread...")
        .setItems(new String[] {"From Musubi", "Other..."}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        sendToDbFriend();
                        break;
                    case 1:
                        sendToExternalFriend();
                        break;
                }
            }
        }).show();
    }

    private void sendToDbFriend() {
        Intent send = new Intent(this, PickContactsActivity.class);
        send.setAction(PickContactsActivity.INTENT_ACTION_INVITE_TO_THREAD);
        send.putExtra("uri", mInternalFeedUri);
        startActivity(send);
    }

    private void sendToExternalFriend() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, "Join me in a Musubi thread: " +
                ThreadRequest.getInvitationUri(this, mExternalFeedUri));
        share.putExtra(Intent.EXTRA_SUBJECT, "Join me on Musubi!");
        share.setType("text/plain");
        startActivity(share);
    }

    public void broadcastNearby() {
        new AlertDialog.Builder(GroupsTabActivity.this)
            .setTitle("Share thread...")
            .setItems(new String[] {"Use Bluetooth (beta)", "Use GPS"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            requestBluetooth();
                            break;
                        case 1:
                            broadcastGps();
                            break;
                    }
                }
            }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BT_BROADCAST) {
            if (resultCode > 0) {
                Toast.makeText(this, "Bluetooth sharing enabled.", 500).show();
                broadcastBluetooth();
            } else {
                return;
            }
        }
        if (requestCode == REQUEST_BT_ENABLE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            } else {
                requestBluetooth();
            }
        }
    }

    public void requestBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bt, REQUEST_BT_ENABLE);
            return;
        }

        final int DISCO_LENGTH = 300;
        Intent discoverableIntent = new
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCO_LENGTH);
        startActivityForResult(discoverableIntent, REQUEST_BT_BROADCAST);
    }

    public void broadcastBluetooth() {
        // BluetoothNdef.share
        Maybe<Group> group = Group.forFeed(this, mInternalFeedUri.toString());
        try {
            Group g = group.get();
            JSONObject json = new JSONObject();
            json.put("name", g.name);
            json.put("dynuri", g.dynUpdateUri);
            BluetoothBeacon.share(this, json.toString().getBytes(), 300);
        } catch (Exception e) {
            Log.e(TAG, "Could not send group invite; no group for " + mInternalFeedUri);
        }
    }

    public void broadcastGps()
    {
        final CharSequence[] items = {"5 minutes", "15 minutes", "1 hour", " 24 hours"};

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupsTabActivity.this);
        builder.setTitle("Choose duration of broadcast");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, final int item) {


                AlertDialog.Builder alert = new AlertDialog.Builder(GroupsTabActivity.this);
                alert.setMessage("Enter a secret key if you want to:");
                final EditText input = new EditText(GroupsTabActivity.this);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final String password = input.getText().toString();

                            myLocation = new MyLocation();

                            locationResult = new MyLocation.LocationResult(){

                            
                                final ProgressDialog dialog = ProgressDialog.show(GroupsTabActivity.this, "", 
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

                                        Intent intent = getIntent();
                                        Long group_id = null;
                                        String group_name = null;
                                        String feed_name = null;
                                        String feed_uri = null;
                                        // TODO: Depracate extras-based access in favor of Data field.
                                        if (intent.hasExtra("group_id")) {
                                            group_id = intent.getLongExtra("group_id", -1);
                                            group_name = intent.getStringExtra("group_name");
                                            feed_name = group_name;
                                            Maybe<Group> maybeG = Group.forId(GroupsTabActivity.this, group_id);
                                            try {
                                                Group g = maybeG.get();
                                                feed_name = g.feedName;
                                            } catch (Exception e) {}
                                            feed_uri = intent.getStringExtra("group_uri");
                                        } else if (getIntent().getType() != null && getIntent().getType().equals(Group.MIME_TYPE)) {
                                            group_id = Long.parseLong(getIntent().getData().getLastPathSegment());
                                            Maybe<Group> maybeG = Group.forId(GroupsTabActivity.this, group_id);
                                            try {
                                                Group g = maybeG.get();
                                                group_name = g.name;
                                                feed_name = g.feedName;
                                                feed_uri = g.dynUpdateUri;
                                            } catch (Exception e) {}
                                        } else if (getIntent().getData().getAuthority().equals("vnd.mobisocial.db")) {
                                            String feedName = getIntent().getData().getLastPathSegment();
                                            Maybe<Group>maybeG = Group.forFeed(GroupsTabActivity.this, feedName);
                                            Group g = null;
                                            try {
                                               g = maybeG.get();
                                                
                                            } catch (Exception e) {
                                                g = Group.createForFeed(GroupsTabActivity.this, feedName);
                                            }
                                            group_name = g.name;
                                            feed_name = g.feedName;
                                            feed_uri = g.dynUpdateUri;
                                            group_id = g.id;
                                        }

                                    
                                        nameValuePairs.add(new BasicNameValuePair("group_name", group_name));
                                        nameValuePairs.add(new BasicNameValuePair("feed_uri", feed_uri));
                                        nameValuePairs.add(new BasicNameValuePair("length", Integer.toString(minutes)));
                                        nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
                                        nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(location.getLongitude())));
                                        nameValuePairs.add(new BasicNameValuePair("password", password));
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
                                        else Log.w(TAG, "Wtf");  

                                        Log.w(TAG, "response: " + response);
                                    }
                                    catch(Exception e) {
                                    }

                                    
                                    dialog.dismiss();
                                }
                            };

                            locationClick();
                            
                        }
                    });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                alert.show();
            
                
                
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/

    
    public MyLocation myLocation;
    public MyLocation.LocationResult locationResult;

    private void locationClick() {
        myLocation.getLocation(GroupsTabActivity.this, locationResult);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mNfc = new Nfc(this);

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  

        Intent intent = getIntent();
        Long group_id = null;
        String group_name = null;
        String feed_name = null;
        String dyn_feed_uri = null;
        // TODO: Depracate extras-based access in favor of Data field.
        if (intent.hasExtra("group_id")) {
            group_id = intent.getLongExtra("group_id", -1);
            group_name = intent.getStringExtra("group_name");
            feed_name = group_name;
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                feed_name = g.feedName;
            } catch (Exception e) {}
            dyn_feed_uri = intent.getStringExtra("group_uri");
        } else if (getIntent().getType() != null && getIntent().getType().equals(Group.MIME_TYPE)) {
            group_id = Long.parseLong(getIntent().getData().getLastPathSegment());
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                group_name = g.name;
                feed_name = g.feedName;
                dyn_feed_uri = g.dynUpdateUri;
            } catch (Exception e) {}
        } else if (getIntent().getData().getAuthority().equals("vnd.mobisocial.db")) {
            String feedName = getIntent().getData().getLastPathSegment();
            Maybe<Group>maybeG = Group.forFeed(this, feedName);
            Group g = null;
            try {
               g = maybeG.get();
                
            } catch (Exception e) {
                g = Group.createForFeed(this, feedName);
            }
            group_name = g.name;
            feed_name = g.feedName;
            dyn_feed_uri = g.dynUpdateUri;
            group_id = g.id;
        }

        if (dyn_feed_uri != null) {
            mNfc.share(NdefFactory.fromUri(dyn_feed_uri));
            Log.w(TAG, dyn_feed_uri);
        }

        mExternalFeedUri = Uri.parse(dyn_feed_uri);
        mInternalFeedUri = Uri.parse(feed_name);

        int color = Feed.colorFor(feed_name);
        
        setTitleFromActivityLabel (R.id.title_text, group_name);
        View titleView = getWindow().findViewById(android.R.id.title);
        if (titleView != null) {
            ViewParent parent = titleView.getParent();
            if (parent != null && parent instanceof View) {
                View parentView = (View) parent;
                parentView.setBackgroundColor(color);
            }
        }

        // Note: If you change this color, also update the cacheColorHint
        // in FeedActivity and ContactsActivity.
        //tabHost.setBackgroundColor(color);
        //tabHost.getBackground().setAlpha(Feed.BACKGROUND_ALPHA);
            
        intent = new Intent().setClass(this, FeedActivity.class);
        intent.putExtra("group_id", group_id);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feed",
            null).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, ContactsActivity.class);
        intent.putExtra("group_id", group_id);
        intent.putExtra("group_name", group_name);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Members",
            null).setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfc.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) return;
    }

    public void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GroupsTabActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });

    }
}





