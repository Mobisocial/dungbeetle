package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.encode.EncodeActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.PickContactsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.view.FeedViews;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Group.InvalidGroupParameters;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;
import edu.stanford.mobisocial.dungbeetle.util.BluetoothBeacon;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;

import edu.stanford.mobisocial.dungbeetle.ui.FeedHomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

/**
 * A UI-less fragment that adds feed actions to the menu and action bar.
 */
public class FeedActionsFragment extends Fragment {
    private static final String TAG = "feedAction";
    private static final int MENU_VIEW = 1041;
    private static final int MENU_SHARE = 1042;

    private Uri mFeedUri;
    private Uri mExternalFeedUri;
    private String mGroupName;
    private boolean mDualPane;
    

    private static final int REQUEST_BT_BROADCAST = 2;
    private static final int REQUEST_BT_ENABLE = 3;
    



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (!getArguments().containsKey(FeedViewFragment.ARG_FEED_URI)) {
            throw new IllegalArgumentException("FeedActionFragment created with no feed_uri argument.");
        }
        mFeedUri = getArguments().getParcelable(FeedViewFragment.ARG_FEED_URI);
        mDualPane = getArguments().getBoolean(FeedViewFragment.ARG_DUAL_PANE, false);

        Group g = Group.forFeedName(getActivity(), mFeedUri.getLastPathSegment());
        DBHelper dbh = DBHelper.getGlobal(getActivity());
        DBIdentityProvider idp = new DBIdentityProvider(dbh);
        RSAPublicKey me = idp.userPublicKey();
        idp.close();
        dbh.close();
        if(g != null) {
            mGroupName = g.name;
            try {
				mExternalFeedUri = Group.makeUriForInvite(g.name, new RSAPublicKey[] {me}, g.pub, g.priv);
			} catch (InvalidGroupParameters e) {
				Toast.makeText(getActivity(), "failed to create group invite", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "failed to create group invite", e);
			}
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item;
        int placement = 0;
        Log.d(TAG, "creating menu " + mDualPane);
        if (mDualPane || MusubiBaseActivity.isDeveloperModeEnabled(getActivity())) {
            item = menu.add(0, MENU_VIEW, placement++, "View");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
        item = menu.add(0, MENU_SHARE, placement++, "Share");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        
    }

    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()) {
            case MENU_VIEW: {
                FeedViews.promptForView(getActivity(), mFeedUri);
                return true;
            }
            case MENU_SHARE: {
                promptForSharing();
                return true;
            }
        }
        return false;
    }
    

    public void promptForSharing() {
        new AlertDialog.Builder(getActivity())
            .setTitle("Share group...")
            .setItems(new String[] {"Send to friend", "Broadcast nearby", "QR code"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            sendToFriend();
                            break;
                        case 1:
                            broadcastNearby();
                            break;
                        case 2:
                            showQR();
                            break;
                    }
                }
            }).show();
    }

    public void sendToFriend() {
        new AlertDialog.Builder(getActivity())
        .setTitle("Share group...")
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
        Intent send = new Intent(getActivity(), PickContactsActivity.class);
        send.setAction(PickContactsActivity.INTENT_ACTION_INVITE_TO_THREAD);
        send.putExtra("uri", mFeedUri);
        startActivity(send);
    }

    private void sendToExternalFriend() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/html");
        share.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Join me in a <a href=\"" + mExternalFeedUri + "\">Musubi thread</a>"));
        share.putExtra(Intent.EXTRA_SUBJECT, "Join me on Musubi!");
        startActivity(share);
    }

    private void broadcastNearby() {
        if (MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            new AlertDialog.Builder(getActivity())
                .setTitle("Share group...")
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
        else {
            broadcastGps();
        }
    }

    private void showQR() {
        Intent qrIntent = new Intent(Intents.Encode.ACTION);
        qrIntent.setClass(getActivity(), EncodeActivity.class);
        qrIntent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        qrIntent.putExtra(Intents.Encode.DATA, mExternalFeedUri);
        startActivity(qrIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BT_BROADCAST) {
            if (resultCode > 0) {
                Toast.makeText(getActivity(), "Bluetooth sharing enabled.", 500).show();
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
        Group g = Group.forFeed(getActivity(), mFeedUri);
        if(g != null) {
            JSONObject json = new JSONObject();
            try {
				json.put("name", g.name);
	            json.put("dynuri", g.dynUpdateUri);
	            BluetoothBeacon.share(getActivity(), json.toString().getBytes(), 300);
			} catch (JSONException e) {
	            Log.e(TAG, "Could not send group invite; JSON failure", e);
				e.printStackTrace();
			}
        } else {
            Log.e(TAG, "Could not send group invite; no group for " + mFeedUri);
        }
    }

    public void broadcastGps()
    {
        final CharSequence[] items = {"5 minutes", "15 minutes", "1 hour", " 24 hours"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose duration of broadcast");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, final int item) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage("Enter a secret key if you want to:");
                final EditText input = new EditText(getActivity());
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final String password = input.getText().toString();
                            myLocation = new MyLocation();
                            locationResult = new MyLocation.LocationResult() {
                                final ProgressDialog dialog = ProgressDialog.show(getActivity(), "", 
                                            "Preparing broadcast...", true);

                                @Override
                                public void gotLocation(final Location location){
                                    //Got the location!
                                    try {
                                        int minutes;
                                        if (item == 0) {
                                            minutes = 5;
                                        } else if (item == 1) {
                                            minutes = 15;
                                        } else if (item == 2) {
                                            minutes = 60;
                                        } else if (item == 3) {
                                            minutes = 1440;
                                        } else {
                                            minutes = 5;
                                        }

                                        Uri uri = new Uri.Builder()
                                            .scheme("http")
                                            .authority("suif.stanford.edu")
                                            .path("dungbeetle/nearby.php").build();
                                        
                                        StringBuffer sb = new StringBuffer();
                                        DefaultHttpClient client = new DefaultHttpClient();
                                        HttpPost httpPost = new HttpPost(uri.toString());

                                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                                        nameValuePairs.add(new BasicNameValuePair("group_name", mGroupName));
                                        nameValuePairs.add(new BasicNameValuePair("feed_uri", mExternalFeedUri.toString()));
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
                                            Toast.makeText(getActivity(), 
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

    public MyLocation myLocation;
    public MyLocation.LocationResult locationResult;

    private void locationClick() {
        myLocation.getLocation(getActivity(), locationResult);
    }
}
