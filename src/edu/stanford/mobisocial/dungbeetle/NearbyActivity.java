package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.Build;
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
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Contact.CursorUser;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;
import android.location.Location;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.net.Uri;
import mobisocial.socialkit.musubi.DbUser;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class NearbyActivity extends ListActivity {
    ArrayList<NearbyItem> mGroupList = new ArrayList<NearbyItem>();
    
    String TAG = "Nearby";
    private int mNearbyMethod = -1;
    private static final int NEARBY_GPS = 1;
    private static final int NEARBY_BLUETOOTH = 2;
    private static final int RESULT_BT_ENABLE = 1;

    public MyLocation myLocation;
    public MyLocation.LocationResult locationResult;
    
    ProgressDialog dialog;
    private NearbyAdapter mAdapter;

    private void locationClick() {
        if (mNearbyMethod == NEARBY_BLUETOOTH) {
            findBluetooth();
        } else if (mNearbyMethod == NEARBY_GPS) {
            dialog.show();
            mGroupList.clear();
            myLocation.getLocation(this, locationResult);   
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new NearbyAdapter(this, R.layout.nearby_groups_item, mGroupList);
        setListAdapter(mAdapter);

        scanBluetoothAsync();
    }

    private void scanBluetoothAsync() {
        List<CursorUser> users = DbContactAttributes.getUsersWithAttribute(this, Contact.ATTR_BT_MAC);
        Log.d(TAG, "checking over " + users.size() + " users.");
        for (CursorUser u : users) {
            Log.d(TAG, "USER " + u.getName());
            // try to connect u.getAttribute(...)
            // if connected, add to list
        }
    }

    
    private class NearbyItem {
        public String group_name, feed_uri;

        public NearbyItem(String name, String uri) {
            group_name = name;
            feed_uri = uri;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_BT_ENABLE){
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                findBluetooth();
            }
        }
    }

    private class NearbyAdapter extends ArrayAdapter<NearbyItem> {
        private ArrayList<NearbyItem> nearby;
        
        public NearbyAdapter(Context context, int textViewResourceId, ArrayList<NearbyItem> groups) {
            super(context, textViewResourceId, groups);
            this.nearby = groups;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = vi.inflate(R.layout.nearby_groups_item, null);
            }
            final NearbyItem g = nearby.get(position);
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
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bt, RESULT_BT_ENABLE);
            return;
        }

        mNearbyMethod = NEARBY_BLUETOOTH;
        // Create a BroadcastReceiver for ACTION_FOUND
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        dialog = new ProgressDialog(NearbyActivity.this);
        dialog.setMessage("Fetching nearby Groups. Please wait...");
        dialog.setCancelable(true);
        dialog.show();

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
            
                dialog.dismiss();
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
                                                mGroupList.add(new NearbyItem(
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
                            BluetoothBeacon.discover(NearbyActivity.this, device, discovered);
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

    private void findGps(final String password) {
        dialog = new ProgressDialog(NearbyActivity.this);
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
                        mGroupList.add(new NearbyItem(group.optString("group_name"), group.optString("feed_uri")));
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
                Toast.makeText(NearbyActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

