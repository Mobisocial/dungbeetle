
package edu.stanford.mobisocial.dungbeetle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Contact.CursorUser;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class NearbyActivity extends ListActivity {
    private static final String TAG = "Nearby";
    private boolean DBG = true;

    private NearbyAdapter mAdapter;
    private ArrayList<NearbyItem> mNearbyList = new ArrayList<NearbyItem>();
    private static final int RESULT_BT_ENABLE = 1;
   
    private GpsScannerTask mGpsScanner;
    private BluetoothScannerTask mBtScanner;
    private MulticastScannerTask mMulticastScanner;
    private MulticastBroadcastTask mMulticastBroadcaster;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        findViewById(R.id.go).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRefresh(null);
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
        mAdapter = new NearbyAdapter(this, R.layout.nearby_groups_item, mNearbyList);
        setListAdapter(mAdapter);

        if (!MusubiBaseActivity.isDeveloperModeEnabled(this)) {
            findViewById(R.id.social).setVisibility(View.GONE);
        } else {
            CheckBox checkbox = (CheckBox)findViewById(R.id.social);
            mMulticastBroadcaster = MulticastBroadcastTask.getInstance(NearbyActivity.this);
            if (mMulticastBroadcaster.isRunning()) {
                checkbox.setChecked(true);
            }
            checkbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // TODO: Generalize to NearbyBroadcaster; do multicast, bt, gps, dns
                        if (buttonView.isChecked()) {
                            if (mMulticastBroadcaster == null) {
                                mMulticastBroadcaster =
                                        MulticastBroadcastTask.getInstance(NearbyActivity.this);
                            }
                            mMulticastBroadcaster.execute();
                        } else {
                            mMulticastBroadcaster.cancel(true);
                            mMulticastBroadcaster = null;
                        }
                    }
                });
        }
    }

    public void onClickRefresh(View view) {
        mNearbyList.clear();
        mAdapter.notifyDataSetChanged();
        scanNearby();
    }

    private void scanNearby() {
        if (mGpsScanner != null) {
            mGpsScanner.cancel(true);
            mMulticastScanner.cancel(true);
            // mBtScanner.cancel(true);
        }

        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        mGpsScanner = new GpsScannerTask(password);
        mMulticastScanner = new MulticastScannerTask();
        // mBtScanner = new BluetoothScannerTask();

        mGpsScanner.execute();
        mMulticastScanner.execute();
        // mBtScanner.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanNearby();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGpsScanner != null) {
            mGpsScanner.cancel(true);
            mMulticastScanner.cancel(true);
            // mBtScanner.cancel(true);

            mGpsScanner = null;
            mMulticastScanner = null;
            // mBtScanner = null;
        }
    }

    private class GpsScannerTask extends AsyncTask<Void, NearbyItem, List<NearbyItem>> {
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
                        if (DBG)
                            Log.d(TAG, "Waiting for location results...");
                        mmLocationResult.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mmResults;
        }

        private final MyLocation.LocationResult mmLocationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(final Location location) {
                if (DBG)
                    Log.d(TAG, "got location, searching for nearby feeds...");
                if (isCancelled()) {
                    synchronized (mmLocationResult) {
                        mmLocationResult.notify();
                    }
                    return;
                }

                // Got the location!
                try {
                    Uri uri = new Uri.Builder().scheme("http").authority("suif.stanford.edu")
                            .path("dungbeetle/nearby.php").build();

                    StringBuffer sb = new StringBuffer();
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(uri.toString());

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location
                            .getLatitude())));
                    nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(location
                            .getLongitude())));
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String response = sb.toString();
                    JSONArray groupsJSON = new JSONArray(response);
                    List<NearbyItem> results = new ArrayList<NearbyItem>(groupsJSON.length());
                    if (DBG)
                        Log.d(TAG, "Got " + groupsJSON.length() + " groups");
                    for (int i = 0; i < groupsJSON.length(); i++) {
                        JSONObject group = new JSONObject(groupsJSON.get(i).toString());
                        results.add(new NearbyItem(NearbyItem.Type.FEED, group.optString("group_name"),
                                Uri.parse(group.optString("feed_uri")), null));
                    }
                    mmResults = results;
                    mmLocationScanComplete = true;
                    synchronized (mmLocationResult) {
                        mmLocationResult.notify();
                    }
                } catch (Exception e) {
                    if (DBG)
                        Log.d(TAG, "Error searching nearby feeds", e);
                }
            }
        };
    }

    /**
     * Scans known bluetooth mac addresses to see if any are nearby.
     * TODO: This is badly broken, use sdp instead.
     *
     */
    private class BluetoothScannerTask extends NearbyTask {
        @Override
        protected List<NearbyItem> doInBackground(Void... params) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
                if (DBG)
                    Log.w(TAG, "insecure bluetooth not supported.");
                // return null;
            }

            List<CursorUser> users = DbContactAttributes.getUsersWithAttribute(NearbyActivity.this,
                    Contact.ATTR_BT_CORRAL_UUID);
            if (DBG)
                Log.d(TAG, "checking " + users.size() + " users to see who's nearby.");
            String mac = null;
            for (CursorUser u : users) {
                if (DBG)
                    Log.d(TAG, "Checking " + u.getName());
                try {
                    long contactId = u.getLocalId();
                    mac = DbContactAttributes.getAttribute(NearbyActivity.this, contactId,
                            Contact.ATTR_BT_MAC);
                    String uuidStr = DbContactAttributes.getAttribute(NearbyActivity.this,
                            contactId, Contact.ATTR_BT_CORRAL_UUID);
                    UUID uuid = UUID.fromString(uuidStr);
                    BluetoothSocket socket = BluetoothAdapter.getDefaultAdapter()
                            .getRemoteDevice(mac).createInsecureRfcommSocketToServiceRecord(uuid);
                    if (DBG)
                        Log.d(TAG, "Bluetooth connecting to " + mac + ":" + uuid);
                    socket.connect();
                    if (DBG)
                        Log.d(TAG, "Bluetooth connected!");
                    if (DBG)
                        Log.d(TAG, "Bluetooth reading...");
                    byte[] headerBytes = new byte[16];
                    int r = socket.getInputStream().read(headerBytes);
                    if (r != headerBytes.length) {
                        throw new IOException("Too few bytes.");
                    }
                    if (DBG)
                        Log.d(TAG, "Read " + new String(headerBytes));
                    publishProgress(new NearbyItem(NearbyItem.Type.PERSON, u.getName(), Contact.uriFor(contactId),
                            Contact.MIME_TYPE));
                    if (DBG)
                        Log.d(TAG, "Bluetooth closing.");
                    socket.close();
                    if (DBG)
                        Log.d(TAG, "Bluetooth closed.");
                } catch (IOException e) {
                    if (DBG)
                        Log.d(TAG, "no connection for " + mac);
                } catch (IllegalArgumentException e) {
                    if (DBG)
                        Log.d(TAG, "Bad uuid", e);
                }
            }
            if (DBG)
                Log.d(TAG, "Done checking bluetooth devices.");
            return null;
        }
    }

    private class MulticastScannerTask extends NearbyTask {
        static final String NEARBY_GROUP = "239.5.5.0";
        static final int NEARBY_PORT = 9178;

        private final Set<Uri> mSeenUris = new HashSet<Uri>();
        private MulticastSocket mSocket;
        private MulticastLock mLock;

        @Override
        protected void onPreExecute() {
            WifiManager wifi = (WifiManager)getSystemService( Context.WIFI_SERVICE );
            if (wifi == null) {
                Log.d(TAG, "No wifi available.");
                return;
            }
            mLock = wifi.createMulticastLock("msb-scanner");
            mLock.acquire();

            try {
                mSocket = new MulticastSocket(NEARBY_PORT);
            } catch (IOException e) {
                Log.w(TAG, "error multicasting", e);
                mSocket = null;
            }

            // Ignore this device's profile:
            mSeenUris.add(FriendRequest.getMusubiUri(NearbyActivity.this));
        }

        @Override
        protected List<NearbyItem> doInBackground(Void... params) {
            try {
                mSocket.joinGroup(InetAddress.getByName(NEARBY_GROUP));
            } catch (IOException e) {
                Log.w(TAG, "Failed to listen on multicast", e);
                mSocket = null;
            }
            while (mSocket != null) {
                if (isCancelled()) {
                    break;
                }
                try {
                    byte[] buf = new byte[2048];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    mSocket.receive(recv);

                    Uri friendUri = null;
                    try {
                        String uriStr = new String(recv.getData(), 0, recv.getLength());
                        friendUri = Uri.parse(uriStr);
                    } catch (Exception e) {
                        continue;
                    }

                    if (mSeenUris.contains(friendUri)) {
                        continue;
                    }

                    // TODO: User user = FriendRequest.parseUri(friendUri);
                    String name = "Unknown";
                    try {
                        JSONObject o = new JSONObject(friendUri.getQueryParameter("profile"));
                        name = o.getString("name");
                    } catch (Exception e) {
                    }
                    publishProgress(new NearbyItem(NearbyItem.Type.PERSON, name, friendUri, null));
                    mSeenUris.add(friendUri);
                } catch (IOException e) {
                    Log.e(TAG, "Error receiving multicast", e);
                    mSocket = null;
                }
            }
            Log.d(TAG, "Done scanning lan");
            mLock.release();
            return null;
        }
    }

    private static class MulticastBroadcastTask extends AsyncTask<Void, Void, Void> {
        private static MulticastBroadcastTask sInstance;
        private static final int SEVEN_SECONDS = 7000;

        private InetAddress mNearbyGroup;
        private MulticastSocket mSocket;
        private final byte[] mBroadcastMsg;
        private boolean mRunning;
        private boolean mDone;

        private MulticastBroadcastTask(Context context) {
            mBroadcastMsg = FriendRequest.getMusubiUri(context).toString().getBytes();
        }

        public static MulticastBroadcastTask getInstance(Context context) {
            if (sInstance == null || sInstance.mDone) {
                sInstance = new MulticastBroadcastTask(context);
            }
            return sInstance;
        }

        @Override
        protected void onPreExecute() {
            try {
                mRunning = true;
                mDone = false;
                mNearbyGroup = InetAddress.getByName(MulticastScannerTask.NEARBY_GROUP);
                mSocket = new MulticastSocket(MulticastScannerTask.NEARBY_PORT);
            } catch (IOException e) {
                Log.w(TAG, "error multicasting", e);
                mSocket = null;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mSocket.joinGroup(mNearbyGroup);
            } catch (IOException e) {
                Log.w(TAG, "Failed to connect to multicast", e);
            }
            while (mSocket != null) {
                if (isCancelled()) {
                    mSocket.disconnect();
                    break;
                }
                try {
                    DatagramPacket profile = new DatagramPacket(mBroadcastMsg,
                            mBroadcastMsg.length, mNearbyGroup, MulticastScannerTask.NEARBY_PORT);
                    Log.d(TAG, "Sending packet");
                    mSocket.send(profile);
                    try {
                        Thread.sleep(SEVEN_SECONDS);
                    } catch (InterruptedException e) {}
                } catch (IOException e) {
                    mSocket = null;
                }
            }
            mRunning = false;
            mDone = true;
            return null;
        }

        public boolean isRunning() {
            return mRunning;
        }
    }

    private abstract class NearbyTask extends AsyncTask<Void, NearbyItem, List<NearbyItem>> {
        @Override
        protected void onProgressUpdate(NearbyItem... values) {
            if (!isCancelled()) {
                mNearbyList.add(values[0]);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(List<NearbyItem> result) {
            if (!isCancelled() && result != null) {
                for (NearbyItem i : result) {
                    mNearbyList.add(i);
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private static class NearbyItem {
        static enum Type { PERSON, FEED };

        public final Type type;
        public final String name;
        public final Uri uri;
        public final String mimeType;

        public NearbyItem(Type type, String name, Uri uri, String mimeType) {
            this.type = type;
            this.name = name;
            this.uri = uri;
            this.mimeType = mimeType;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
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
        if (requestCode == RESULT_BT_ENABLE) {
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
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = vi.inflate(R.layout.nearby_groups_item, null);
            }
            final NearbyItem g = nearby.get(position);
            TextView text = (TextView) row.findViewById(R.id.name_text);
            text.setText(g.name);

            if (g.type == NearbyItem.Type.PERSON) {
                long cid = FriendRequest.getExistingContactId(NearbyActivity.this, g.uri);
                if (cid != -1) {
                    try {
                        Bitmap img = Contact.forId(NearbyActivity.this, cid).get().picture;
                        ((ImageView)row.findViewById(R.id.icon)).setImageBitmap(img);
                    } catch (NoValError e) {}
                }
            }
            return row;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        NearbyItem g = mAdapter.getItem(position);
        if (g.type == NearbyItem.Type.PERSON) {
            long cid = FriendRequest.getExistingContactId(this, g.uri);
            if (cid != -1) {
                Contact.view(this, cid);
            }
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(g.uri, g.mimeType);
        intent.setPackage(getPackageName());
        startActivity(intent);
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
                                                mNearbyList.add(new NearbyItem(NearbyItem.Type.FEED,
                                                        obj.getString("name"), Uri.parse(obj
                                                                .getString("dynuri")), null));
                                                mAdapter.notifyDataSetChanged();
                                            } catch (JSONException e) {
                                                Log.e(TAG,
                                                        "Error getting group info over bluetooth",
                                                        e);
                                            }
                                        }
                                    });
                                }
                            };
                            // Get the BluetoothDevice object from the Intent
                            BluetoothDevice device = intent
                                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            BluetoothBeacon.discover(NearbyActivity.this, device, discovered);
                        };
                    }.start();
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    unregisterReceiver(this);
                }
            }
        };

        registerReceiver(receiver, filter); // Don't forget to unregister during
                                            // onDestroy
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
