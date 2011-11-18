package edu.stanford.mobisocial.dungbeetle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Contact.CursorUser;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;

public class NearbyActivity extends ListActivity {
    ArrayList<NearbyItem> mGroupList = new ArrayList<NearbyItem>();
    
    String TAG = "Nearby";
    private boolean DBG = true;
    private static final int RESULT_BT_ENABLE = 1;

    private NearbyAdapter mAdapter;
    private GpsScannerTask mGpsScanner;
    private BluetoothScannerTask mBtScanner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        findViewById(R.id.go).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanNearby();
            }
        });
        findViewById(R.id.qr).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator.initiateScan(NearbyActivity.this);
            }
        });
        DBG = MusubiBaseActivity.isDeveloperModeEnabled(this);
        MusubiBaseActivity.doTitleBar(this, "Nearby");
        mAdapter = new NearbyAdapter(this, R.layout.nearby_groups_item, mGroupList);
        setListAdapter(mAdapter);
        scanNearby();
    }

    public void onClickRefresh(View view) {
        mGroupList.clear();
        mAdapter.notifyDataSetChanged();
        scanNearby();
    }

    private void scanNearby() {
        if (mGpsScanner != null) {
            mGpsScanner.cancel(true);
            // mBtScanner.cancel(true);
        }

        String password = ((EditText)findViewById(R.id.password)).getText().toString();
        mGpsScanner = new GpsScannerTask(password);
        mBtScanner = new BluetoothScannerTask();

        mGpsScanner.execute();
        // mBtScanner.execute();
    }

    private class GpsScannerTask extends AsyncTask<Void, Void, List<NearbyItem>> {
        private final String mmPassword;
        private final MyLocation mmMyLocation;
        private boolean mmLocationScanComplete = false;
        private List<NearbyItem> mmResults;

        GpsScannerTask(String password) {
            mmPassword = password;
            mmMyLocation = new MyLocation();
        }

        @Override
        protected void onPreExecute() {
            mmMyLocation.getLocation(NearbyActivity.this, mmLocationResult);
        }

        @Override
        protected List<NearbyItem> doInBackground(Void... params) {
            while (!mmLocationScanComplete) {
                synchronized (mmLocationResult) {
                    try {
                        if (DBG) Log.d(TAG, "Waiting for location results...");
                        mmLocationResult.wait();
                    } catch (InterruptedException e) {}
                }
            }
            return mmResults;
        }

        @Override
        protected void onPostExecute(List<NearbyItem> result) {
            if (result != null) {
                for (NearbyItem i : result) {
                    mGroupList.add(i);
                }
            }
            Log.d(TAG, "I so updated, " + mGroupList.size());
            mAdapter.notifyDataSetChanged();
        }

        private final MyLocation.LocationResult mmLocationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(final Location location) {
                if (DBG) Log.d(TAG, "got location, searching for nearby feeds...");
                if (isCancelled()) {
                    synchronized (mmLocationResult) {
                        mmLocationResult.notify();
                    }
                    return;
                }

                //Got the location!
                try {
                    Uri uri = new Uri.Builder()
                        .scheme("http")
                        .authority("suif.stanford.edu")
                        .path("dungbeetle/nearby.php").build();

                    StringBuffer sb = new StringBuffer();
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(uri.toString());

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
                    nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(location.getLongitude())));
                    nameValuePairs.add(new BasicNameValuePair("password", mmPassword));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    try {
                        HttpResponse execute = client.execute(httpPost);
                        InputStream content = execute.getEntity().getContent();
                        BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                        String s = "";
                        while ((s = buffer.readLine()) != null) {

                            if (isCancelled()) {
                                synchronized (mmLocationResult) {
                                    mmLocationResult.notify();
                                }
                                return;
                            }

                            sb.append(s);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    String response = sb.toString();
                    JSONArray groupsJSON = new JSONArray(response);
                    List<NearbyItem> results = new ArrayList<NearbyItem>(groupsJSON.length());
                    if (DBG) Log.d(TAG, "Got " + groupsJSON.length() + " groups");
                    for (int i = 0; i < groupsJSON.length(); i++) {
                        JSONObject group = new JSONObject(groupsJSON.get(i).toString());
                        results.add(new NearbyItem(group.optString("group_name"),
                                Uri.parse(group.optString("feed_uri")), null));
                    }
                    mmResults = results;
                    mmLocationScanComplete = true;
                    synchronized (mmLocationResult) {
                        mmLocationResult.notify();
                    }
                }
                catch(Exception e) {
                    if (DBG) Log.d(TAG, "Error searching nearby feeds", e);
                }
            }
        };
    }

    private class BluetoothScannerTask extends AsyncTask<Void, NearbyItem, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
                if (DBG) Log.w(TAG, "insecure bluetooth not supported.");
                //return null;
            }

            List<CursorUser> users = DbContactAttributes.getUsersWithAttribute(
                    NearbyActivity.this, Contact.ATTR_BT_CORRAL_UUID);
            if (DBG) Log.d(TAG, "checking " + users.size() + " users to see who's nearby.");
            String mac = null;
            for (CursorUser u : users) {
                if (DBG) Log.d(TAG, "Checking " + u.getName());
                try {
                    long contactId = u.getLocalId();
                    mac = DbContactAttributes.getAttribute(
                            NearbyActivity.this, contactId, Contact.ATTR_BT_MAC);
                    String uuidStr = DbContactAttributes.getAttribute(
                            NearbyActivity.this, contactId, Contact.ATTR_BT_CORRAL_UUID);
                    UUID uuid = UUID.fromString(uuidStr);
                    BluetoothSocket socket = BluetoothAdapter.getDefaultAdapter()
                            .getRemoteDevice(mac).createInsecureRfcommSocketToServiceRecord(uuid);
                    if (DBG) Log.d(TAG, "Bluetooth connecting to " + mac + ":" + uuid);
                    socket.connect();
                    if (DBG) Log.d(TAG, "Bluetooth connected!");
                    if (DBG) Log.d(TAG, "Bluetooth reading...");
                    byte[] headerBytes = new byte[16];
                    int r = socket.getInputStream().read(headerBytes);
                    if (r != headerBytes.length) {
                        throw new IOException("Too few bytes.");
                    }
                    if (DBG) Log.d(TAG, "Read " + new String(headerBytes));
                    publishProgress(new NearbyItem(u.getName(),
                            Contact.uriFor(contactId), Contact.MIME_TYPE));
                    if (DBG) Log.d(TAG, "Bluetooth closing.");
                    socket.close();
                    if (DBG) Log.d(TAG, "Bluetooth closed.");
                } catch (IOException e) {
                    if (DBG) Log.d(TAG, "no connection for " + mac);
                } catch (IllegalArgumentException e) {
                    if (DBG) Log.d(TAG, "Bad uuid", e);
                }
            }
            if (DBG) Log.d(TAG, "Done checking bluetooth devices.");
            return null;
        }

        @Override
        protected void onProgressUpdate(NearbyItem... values) {
            mGroupList.add(values[0]);
            mAdapter.notifyDataSetChanged();
        }
    }

    private class NearbyItem {
        public final String name;
        public final Uri feedUri;
        public final String mimeType;

        public NearbyItem(String name, Uri uri, String mimeType) {
            this.name = name;
            this.feedUri = uri;
            this.mimeType = mimeType;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            try {
                Uri uri = Uri.parse(result.getContents());
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setPackage(getPackageName());
                startActivity(i);
                finish();
            } catch (IllegalArgumentException e) {
            }
            return;
        }
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
            text.setText(g.name);

            row.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(g.feedUri, g.mimeType);
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
                                                mGroupList.add(new NearbyItem(
                                                        obj.getString("name"),
                                                        Uri.parse(obj.getString("dynuri")),
                                                        null));
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

    @SuppressWarnings("unused")
    private void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NearbyActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

