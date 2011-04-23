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
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.util.HTTPDownloadTextFileTask;
import mobisocial.nfc.NdefHandler;
import mobisocial.nfc.Nfc;
import java.security.PublicKey;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import java.util.BitSet;
import com.skjegstad.utils.BloomFilter;
import android.util.Base64;


public class DungBeetleActivity extends TabActivity
{

    public static final String TAG = "DungBeetleActivity";
    public static final String SHARE_SCHEME = "db-share-contact";
    public static final String GROUP_SESSION_SCHEME = "dungbeetle-group-session";
    public static final String GROUP_SCHEME = "dungbeetle-group";
    public static final String AUTO_UPDATE_URL_BASE = "http://mobisocial.stanford.edu/files";
    public static final String AUTO_UPDATE_METADATA_FILE = "dungbeetle_version.json";
    public static final String AUTO_UPDATE_APK_FILE = "dungbeetle-debug.apk";
    private Nfc mNfc;
	private NotificationManager mNotificationManager;

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
        setContentView(R.layout.main);
        startService(new Intent(this, DungBeetleService.class));

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  
        Intent intent;  


        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ContactsActivity.class);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Contacts",
            null).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, ObjectsActivity.class);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feed",
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
        
        tabHost.setCurrentTab(0);

        mNfc = new Nfc(this);
        mNfc.addNdefHandler(new NdefHandler() {
                public int handleNdef(final NdefMessage[] messages){
                    DungBeetleActivity.this.runOnUiThread(new Runnable(){
                            public void run(){
                                doHandleNdef(messages);
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
    }

    protected void doHandleNdef(NdefMessage[] messages){
        if(messages.length != 1 || messages[0].getRecords().length != 1){
            Toast.makeText(this, "Oops! expected a single Uri record. ",
                           Toast.LENGTH_SHORT).show();
            return;
        }
        String uriStr = new String(messages[0].getRecords()[0].getPayload());
        Uri uri = Uri.parse(uriStr);

        if(uri == null){
            Toast.makeText(this, "Null uri!", Toast.LENGTH_SHORT).show();
        }
        else{
            if(uri.getScheme().equals(SHARE_SCHEME)){
                Intent intent = new Intent().setClass(this, HandleNfcContact.class);
                intent.setData(uri);
                startActivity(intent);
            }
            else if(uri.getScheme().equals(GROUP_SESSION_SCHEME)){
                Intent intent = new Intent().setClass(this, HandleGroupSessionActivity.class);
                intent.setData(uri);
                startActivity(intent);
            }
            else{
                Toast.makeText(this, "Unrecognized uri scheme: " + uri.getScheme(), Toast.LENGTH_SHORT).show();
            }
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

        // Don't check for updates too frequently...
        long t = new Date().getTime();
        if((t - lastUpdateCheckTime) > 30000){
            CheckForUpdatesTask task = new CheckForUpdatesTask();
            task.execute(AUTO_UPDATE_URL_BASE + "/" + AUTO_UPDATE_METADATA_FILE);
            lastUpdateCheckTime = t;
        }
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
    }
}




