package edu.stanford.mobisocial.dungbeetle;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;
import edu.stanford.mobisocial.dungbeetle.util.HTTPDownloadTextFileTask;
import java.util.BitSet;
import java.util.Date;
import mobisocial.nfc.NdefHandler;
import mobisocial.nfc.Nfc;
import org.json.JSONException;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import org.json.JSONObject;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.SharedPreferences;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.util.List;

public class DungBeetleActivity extends DashboardActivity
{
    private static final boolean DBG = true;
    public static final String TAG = "DungBeetleActivity";
    public static final String SHARE_SCHEME = "db-share-contact";
    public static final String GROUP_SESSION_SCHEME = "dungbeetle-group-session";
    public static final String GROUP_SCHEME = "dungbeetle-group";
    public static final String AUTO_UPDATE_URL_BASE = "http://mobisocial.stanford.edu/files";
    public static final String AUTO_UPDATE_METADATA_FILE = "dungbeetle_version.json";
    public static final String AUTO_UPDATE_APK_FILE = "dungbeetle-debug.apk";

    public static final String PREFS_NAME = "DungBeetlePrefsFile";
    
    private Nfc mNfc;
	private NotificationManager mNotificationManager;

	private Intent DBServiceIntent;

    private class CheckForUpdatesTask extends HTTPDownloadTextFileTask {
        @Override
        public void onPostExecute(String result) {
            try{
                JSONObject obj = new JSONObject(result);
                int versionCode = obj.getInt("versionCode");
                String versionName = obj.getString("versionName");
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(
                        getPackageName(),PackageManager.GET_META_DATA);
                    if(pInfo.versionCode < versionCode){
                        Toast.makeText(DungBeetleActivity.this,
                                       "Newer version, " + versionName + 
                                       ", found. See notification.", 
                                       Toast.LENGTH_SHORT).show();
                        notifyApkDownload(AUTO_UPDATE_URL_BASE + "/" + AUTO_UPDATE_APK_FILE);
                    }
                    else if(pInfo.versionCode == versionCode){
                        Log.i(TAG, "Up to date.");
                    }
                    else {
                        Toast.makeText(DungBeetleActivity.this, 
                                       "Weird. Local version newer than autoupdate version.", 
                                       Toast.LENGTH_SHORT).show();
                    }
                    
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            catch(JSONException e){
                Log.e(TAG, "Failed to load auto-update info.", e);
            }
        }
    }

    private void notifyApkDownload(String url){
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Notification notification = new Notification(
            R.drawable.icon, 
            "Update available.", System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(
            this, 
            "Update available.", 
            "Click to download latest version.", contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(0, notification);
    }

    
  
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // TODO: Hack.
        try {
            if (getIntent().hasExtra(AppReference.EXTRA_APPLICATION_ARGUMENT)) {
                getIntent().setData(Uri.parse(getIntent().getStringExtra(AppReference.EXTRA_APPLICATION_ARGUMENT)));
            }
        } catch (ClassCastException e) {}
        

        setContentView(R.layout.activity_home);
        DBServiceIntent = new Intent(this, DungBeetleService.class);
        startService(DBServiceIntent);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean firstLoad = settings.getBoolean("firstLoad", true);
        if (firstLoad) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Thank you for trying out Stanford Mobisocial's new software DungBeetle! Would you like to actively participate in our beta test? Press yes to receive e-mail updates about our progress.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try{
                            Uri.Builder b = new Uri.Builder();
                            b.scheme("http");
                            b.authority("suif.stanford.edu");
                            b.path("dungbeetle/emails.php");
                            Uri uri = b.build();

                            StringBuffer sb = new StringBuffer();
                            DefaultHttpClient client = new DefaultHttpClient();
                            HttpPost httpPost = new HttpPost(uri.toString());

                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

                                    
                            DBHelper helper = new DBHelper(DungBeetleActivity.this);
                            DBIdentityProvider ident = new DBIdentityProvider(helper);
                            nameValuePairs.add(new BasicNameValuePair("email", ident.userEmail()));
                            
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            
                            HttpResponse execute = client.execute(httpPost);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        
                        Toast.makeText(DungBeetleActivity.this, "Thank you for signing up!",
                                       Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                    })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                });
            AlertDialog alert = builder.create();
            alert.show();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("firstLoad", false);
            editor.commit();
        }

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        /*TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  
        Intent intent;  


        intent = new Intent().setClass(this, FeedListActivity.class);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feeds",
            null).setContent(intent);
        tabHost.addTab(spec);


        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ContactsActivity.class);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Contacts",
            null).setContent(intent);
        tabHost.addTab(spec);

		
        // Do the same for the other tabs
        
        intent = new Intent().setClass(this, ProfileActivity.class);
        intent.putExtra("contact_id", Contact.MY_ID);
        spec = tabHost.newTabSpec("view_profile").setIndicator("Profile",
                                                               null)
            .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, GroupsActivity.class);
        spec = tabHost.newTabSpec("groups").setIndicator("Groups",
                                                         null)
            .setContent(intent);
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);*/

        mNfc = new Nfc(this);
        // TODO: Combine doHandleInput calls in onNewIntent.
        doHandleInput(getIntent().getData());
        mNfc.addNdefHandler(new NdefHandler() {
                public int handleNdef(final NdefMessage[] messages){
                    DungBeetleActivity.this.runOnUiThread(new Runnable(){
                            public void run(){
                                doHandleInput(uriFromNdef(messages));
                            }
                        });
                    return NDEF_CONSUME;
                }
            });

        mNfc.setOnTagWriteListener(new Nfc.OnTagWriteListener(){
                public void onTagWrite(final int status){
                    DungBeetleActivity.this.runOnUiThread(new Runnable(){
                            public void run(){
                                if(status == WRITE_OK){
                                    Toast.makeText(DungBeetleActivity.this, "Wrote successfully!",
                                                   Toast.LENGTH_SHORT).show();
                                }
                                else if(status == WRITE_ERROR_READ_ONLY){
                                    Toast.makeText(DungBeetleActivity.this, "Can't write read-only tag!",
                                                   Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(DungBeetleActivity.this, "Failed to write!",
                                                   Toast.LENGTH_SHORT).show();
                                }
                                pushContactInfoViaNfc();
                            }
                        }); 
                }
            });
        
        pushContactInfoViaNfc();


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action))
        {
            if (extras.containsKey(Intent.EXTRA_STREAM))
            {
                try
                {
                    UIHelpers.showGroupPicker(DungBeetleActivity.this, null, intent);
                } catch (Exception e)
                {
                    Log.e(this.getClass().getName(), e.toString());
                }

            }
        }
    }

	
    public Uri uriFromNdef(NdefMessage... messages) {
        if(messages.length == 0){
            return null;
        }
        
       return Uri.parse(new String(messages[0].getRecords()[0].getPayload()));
    }

    protected void doHandleInput(Uri uri){
        if(uri == null){
            return;
        }

        if (DBG) Log.d(TAG, "launching dungbeetle with uri " + uri);
        if(uri.getScheme().equals(SHARE_SCHEME)
                || uri.getSchemeSpecificPart().startsWith(FriendRequest.PREFIX_JOIN)){
            Intent intent = new Intent().setClass(this, HandleNfcContact.class);
            intent.setData(uri);
            startActivity(intent);
        } else if(uri.getScheme().equals(GROUP_SESSION_SCHEME)){
            Intent intent = new Intent().setClass(this, HandleGroupSessionActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
        else if (uri.getScheme().equals("content")) {
            if (uri.getAuthority().equals("vnd.mobisocial.db")) {
                if (uri.getPath().startsWith("/feed")) {
                    Intent view = new Intent(Intent.ACTION_VIEW);
                    view.addCategory(Intent.CATEGORY_DEFAULT);
                    view.setData(uri);
                    // TODO: fix in AndroidManifest.
                    //view.setClass(this, FeedActivity.class);
                    view.setClass(this, GroupsTabActivity.class);
                    startActivity(view);
                    finish();
                    return;
                }
    		}
        } else if  (!acceptInboundContactInfo()) {
            Toast.makeText(this, "Unrecognized uri scheme: " + uri.getScheme(), Toast.LENGTH_SHORT).show();
        }

        // Re-push the contact info ndef
        pushContactInfoViaNfc();
    }

    public void writeGroupToTag(Uri uri){
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {},
            uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.enableTagWriteMode(ndef);
        Toast.makeText(this, 
                       "Touch a tag to write the group...", 
                       Toast.LENGTH_SHORT).show();
    }
    

    public static byte[] toByteArray(BitSet bits) {
        byte[] bytes = new byte[bits.length()/8+1];
        for(int i = 0; i < bits.length(); i++) {
            if(bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8);
            }
        }
        return bytes;
    }

    public void pushGroupInfoViaNfc(Uri uri){
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {},
            uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.share(ndef);
    }

    public void pushContactInfoViaNfc(){
    	
    	/*
    	BloomFilter friendsFilter = Helpers.getFriendsBloomFilter(this);
    	builder.appendQueryParameter("filterData", Base64.encodeToString(toByteArray(friendsFilter.getBitSet()), Base64.DEFAULT));
        builder.appendQueryParameter("bitSetSize", Integer.toString(friendsFilter.size()));
        builder.appendQueryParameter("expectedNumberOfFilterElements", Integer.toString(friendsFilter.getExpectedNumberOfElements()));
        builder.appendQueryParameter("actualNumberOfFilterElements", Integer.toString(friendsFilter.count()));
        */  
    	Uri uri = FriendRequest.getInvitationUri(this);
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {},
            uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.share(ndef);
        //Toast.makeText(this, "Touch phones with your friend!", Toast.LENGTH_SHORT).show();
    }

    public boolean acceptInboundContactInfo() {
        if (getIntent().getData() == null) {
            // TODO: convert if(getFoo().doBar()) into if (getFoo() != null && getFoo().doBar())
            return false;
        }
        if (getIntent().getData().getAuthority().equals("mobisocial.stanford.edu")) {
            Uri uri = getIntent().getData();
            List<String> segments = uri.getPathSegments();
            if (segments.contains("join")) {
                FriendRequest.acceptFriendRequest(this, getIntent().getData());
                return true;
            } else if (segments.contains("thread")) {
                ThreadRequest.acceptThreadRequest(this, getIntent().getData());
                return true;
            }
            // TODO, update bigtime
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }


    private long lastUpdateCheckTime = 0;
    @Override
    public void onResume() {
        super.onResume();
        mNfc.onResume(this);
        pushContactInfoViaNfc();

        // Don't check for updates too frequently...
        /*long t = new Date().getTime();
        if((t - lastUpdateCheckTime) > 30000){
            CheckForUpdatesTask task = new CheckForUpdatesTask();
            task.execute(AUTO_UPDATE_URL_BASE + "/" + AUTO_UPDATE_METADATA_FILE);
            lastUpdateCheckTime = t;
        }*/
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) {
            return;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //stopService(DBServiceIntent);
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    public Uri ANULL (Uri u) {
        return (u == null) ? Uri.parse("urn:"): u;
    }

   /* private void toast(final String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }*/
}




