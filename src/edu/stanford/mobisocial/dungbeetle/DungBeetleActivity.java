package edu.stanford.mobisocial.dungbeetle;
import java.util.Date;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import edu.stanford.mobisocial.dungbeetle.util.HTTPDownloadTextFileTask;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import org.json.JSONException;
import org.json.JSONObject;
import android.nfc.NdefMessage;
import android.widget.Toast;
import edu.stanford.mobisocial.nfc.Nfc;
import android.nfc.NdefRecord;
import android.net.Uri;
import java.security.PublicKey;
import android.view.View;
import android.widget.Button;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.view.View.OnClickListener;

public class DungBeetleActivity extends TabActivity
{

    public static final String TAG = "DungBeetleActivity";
    private Nfc mNfc;
	private NotificationManager mNotificationManager;

    private class CheckForUpdatesTask extends HTTPDownloadTextFileTask {

        final String baseUrl;

        public CheckForUpdatesTask(String base) {
            baseUrl = base;
        }

        public void execute(){ 
            execute(baseUrl + "/" + "dungbeetle_version.json");
        }

        @Override
        public void onPostExecute(String result) {
            try{
                JSONObject obj = new JSONObject(result);
                int versionCode = obj.getInt("versionCode");
                String versionName = obj.getString("versionName");
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(
                        getPackageName(),PackageManager.GET_META_DATA);
                    System.out.println(pInfo.versionCode);
                    if(pInfo.versionCode < versionCode){
                        Toast.makeText(DungBeetleActivity.this,
                                       "Newer version," + versionName + 
                                       ", found!", Toast.LENGTH_SHORT).show();
                        notifyApkDownload(baseUrl + "/" + "dungbeetle-debug.apk");
                    }
                    else if(pInfo.versionCode == versionCode){
                        Toast.makeText(DungBeetleActivity.this, 
                                       "Up to date.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(DungBeetleActivity.this, 
                                       "Weird. Local version newer than autoupdate version.", Toast.LENGTH_SHORT).show();
                    }
                    
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }
            catch(JSONException e){
                Toast.makeText(DungBeetleActivity.this, 
                               "Failed to load auto-update info.",
                               Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void notifyApkDownload(String url){
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
        Notification notification = new Notification(
            R.drawable.icon, 
            "Update available.", System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
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
        spec = tabHost.newTabSpec("profile").setIndicator("Profile",
                                                          null)
            .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, GroupsActivity.class);
        spec = tabHost.newTabSpec("groups").setIndicator("Groups",
                                                         null)
            .setContent(intent);
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);


        Button button = (Button)findViewById(R.id.share_info_button);
        button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    shareContactInfo();
                }
            });

        mNfc = new Nfc(this);
        mNfc.addNdefHandler(new Nfc.NdefHandler(){
                public int handleNdef(final NdefMessage[] messages){
                    DungBeetleActivity.this.runOnUiThread(new Runnable(){
                            public void run(){
                                doHandleNdef(messages);
                            }
                        });
                    return NDEF_CONSUME;
                }
            });


    }

    protected void doHandleNdef(NdefMessage[] messages){
        if(messages.length != 1 || messages[0].getRecords().length != 1){
            Toast.makeText(this, "Oops! expected a single Uri record. ",
                           Toast.LENGTH_SHORT).show();
            return;
        }
        String uriStr = new String(messages[0].getRecords()[0].getPayload());
        Uri myUri = Uri.parse(uriStr);
        if(myUri == null || !myUri.getScheme().equals(ContactsActivity.SHARE_SCHEME)){
            Toast.makeText(this, "Received record without valid Uri!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent().setClass(this, HandleNfcContact.class);
        intent.setData(myUri);
        startActivity(intent);
    }


    protected void shareContactInfo(){
        DBHelper helper = new DBHelper(this);
        IdentityProvider ident = new DBIdentityProvider(helper);
        String name = ident.userName();
        String email = ident.userEmail();
        PublicKey pubKey = ident.userPublicKey();
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContactsActivity.SHARE_SCHEME);
        builder.authority("dungbeetle");
        builder.appendQueryParameter("name", name);
        builder.appendQueryParameter("email", email);
        builder.appendQueryParameter("publicKey", DBIdentityProvider.publicKeyToString(pubKey));
        Uri uri = builder.build();
        NdefRecord urlRecord = new NdefRecord(
            NdefRecord.TNF_ABSOLUTE_URI, 
            NdefRecord.RTD_URI, new byte[] {},
            uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.share(ndef);
        Toast.makeText(this, "Touch phones with your friend!", Toast.LENGTH_SHORT).show();
        helper.close();
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
            CheckForUpdatesTask task = new CheckForUpdatesTask("http://mobisocial.stanford.edu/files");
            task.execute();
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




