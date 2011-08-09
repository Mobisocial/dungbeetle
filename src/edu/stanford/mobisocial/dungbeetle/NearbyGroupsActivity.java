package edu.stanford.mobisocial.dungbeetle;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import android.location.Location;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.net.Uri;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

public class NearbyGroupsActivity extends ListActivity {
    String[] listItems = {"sup dawg", "Restore from Dropbox", "Wipe Data (Keep identity)", "Start from Scratch"};
    ArrayList<GroupItem> mGroupList = new ArrayList<GroupItem>();
    
    String TAG = "Nearby Groups";

    /*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, DungBeetleActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
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


    public void onClickRefresh (View v)
    {
        
        locationClick();
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/

    public MyLocation myLocation;
    public MyLocation.LocationResult locationResult;
    
    ProgressDialog dialog;
    private GroupAdapter mAdapter;

    private void locationClick() {
        dialog.show();
        mGroupList.clear();
        myLocation.getLocation(this, locationResult);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby_groups);
        setTitleFromActivityLabel (R.id.title_text);
        //setListAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems));
        mAdapter = new GroupAdapter(this, R.layout.nearby_groups_item, mGroupList);
        setListAdapter(mAdapter);

        new AlertDialog.Builder(this)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
            .setTitle("Choose method...")
            .setItems(new String[] { "Bluetooth" , "Gps" }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            findBluetooth();
                            break;
                        case 1:
                            promptGps();
                            break;
                    }
                }
            }).create().show();
    }

    
    private class GroupItem {
        public String group_name, feed_uri;

        public GroupItem(String name, String uri) {
            group_name = name;
            feed_uri = uri;
        }
    }

    private class GroupAdapter extends ArrayAdapter<GroupItem> {
        private ArrayList<GroupItem> groups;
        
        public GroupAdapter(Context context, int textViewResourceId, ArrayList<GroupItem> groups) {
            super(context, textViewResourceId, groups);
            this.groups = groups;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = vi.inflate(R.layout.nearby_groups_item, null);
            }
            final GroupItem g = groups.get(position);
            TextView text = (TextView) row.findViewById(R.id.name_text);
            text.setText(g.group_name);

            row.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    Log.w(TAG, g.feed_uri);
                    Intent intent = new Intent(getApplicationContext(), HandleGroupSessionActivity.class);
                    intent.setData(Uri.parse(g.feed_uri));
                    startActivity(intent);
                }
            });
            return row;
        }
    }

    private void findBluetooth() {
        // Create a BroadcastReceiver for ACTION_FOUND
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    new Thread() {
                        public void run() {
                            BluetoothBeacon.OnDiscovered discovered = new BluetoothBeacon.OnDiscovered() {
                                @Override
                                public void onDiscovered(final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject obj = new JSONObject(new String(data));
                                                mGroupList.add(new GroupItem(
                                                        obj.getString("name"), obj.getString("dynuri")));
                                                mAdapter.notifyDataSetChanged();
                                            } catch (JSONException e) {
                                                Log.e(TAG, "Error getting group info over bluetooth", e);
                                            }
                                        }
                                    });
                                }
                            };
                            // Get the BluetoothDevice object from the Intent
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            BluetoothBeacon.discover(NearbyGroupsActivity.this, device, discovered);
                        };
                    }.start();
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    unregisterReceiver(this);
                }
            }
        };

        
        registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy   
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        Toast.makeText(this, "Scanning Bluetooth...", 500).show();
    }

    private void promptGps() {
        // Get password
        AlertDialog.Builder builder = new AlertDialog.Builder(NearbyGroupsActivity.this);
        builder.setMessage("Enter your secret key if you have one:");
        final EditText passwordInput = new EditText(NearbyGroupsActivity.this);
        builder.setView(passwordInput);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String password = passwordInput.getText().toString();
                    findGps(password);
                }
            });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    private void findGps(final String password) {
        dialog = new ProgressDialog(NearbyGroupsActivity.this);
        dialog.setMessage("Fetching nearby Groups. Please wait...");
        dialog.setCancelable(false);
        myLocation = new MyLocation();
        
        Intent intent = getIntent();

        locationResult = new MyLocation.LocationResult(){
            @Override
            public void gotLocation(final Location location){
                //Got the location!
                try {
                    Uri.Builder b = new Uri.Builder();
                    b.scheme("http");
                    b.authority("suif.stanford.edu");
                    b.path("dungbeetle/nearby.php");
                    Uri uri = b.build();
                    
                    StringBuffer sb = new StringBuffer();
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(uri.toString());

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                
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
                    JSONArray groupsJSON = new JSONArray(response);
                    for(int i = 0; i < groupsJSON.length(); i++) {
                        JSONObject group = new JSONObject(groupsJSON.get(i).toString());
                        mGroupList.add(new GroupItem(group.optString("group_name"), group.optString("feed_uri")));
                    }
                    mAdapter.notifyDataSetChanged();
                }
                catch(Exception e) {
                }

                
                dialog.dismiss();
            }
        };

        locationClick();
    }

    private void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NearbyGroupsActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

