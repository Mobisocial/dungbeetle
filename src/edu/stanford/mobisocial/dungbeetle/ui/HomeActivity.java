package edu.stanford.mobisocial.dungbeetle.ui;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.NdefHandler;
import mobisocial.nfc.Nfc;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleService;
import edu.stanford.mobisocial.dungbeetle.GroupsActivity;
import edu.stanford.mobisocial.dungbeetle.HandleGroupSessionActivity;
import edu.stanford.mobisocial.dungbeetle.HandleNfcContact;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.NearbyGroupsActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.SettingsActivity;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;
import edu.stanford.mobisocial.dungbeetle.social.ThreadRequest;

public class HomeActivity extends MusubiBaseActivity {
    public static final boolean DBG = true;
    public static final String TAG = "DungBeetleActivity";
    public static final String SHARE_SCHEME = "db-share-contact";
    public static final String GROUP_SESSION_SCHEME = "dungbeetle-group-session";
    public static final String GROUP_SCHEME = "dungbeetle-group";
    public static final String AUTO_UPDATE_URL_BASE = "http://mobisocial.stanford.edu/files";
    public static final String AUTO_UPDATE_METADATA_FILE = "dungbeetle_version.json";
    public static final String AUTO_UPDATE_APK_FILE = "dungbeetle-debug.apk";

    public static final String PREFS_NAME = "DungBeetlePrefsFile";
    
    private Nfc mNfc;
	private Intent DBServiceIntent;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // TODO: Hack.
        try {
            if (getIntent().hasExtra(AppState.EXTRA_APPLICATION_ARGUMENT)) {
                getIntent().setData(Uri.parse(getIntent().getStringExtra(AppState.EXTRA_APPLICATION_ARGUMENT)));
            }
        } catch (ClassCastException e) {}

        setContentView(R.layout.activity_home);
        MusubiBaseActivity.doTitleBar(this);
        DBServiceIntent = new Intent(this, DungBeetleService.class);
        startService(DBServiceIntent);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String sBaseHues = settings.getString("baseHues", null);
    	String[] aBaseHues;
        if(sBaseHues != null) {
        	aBaseHues = sBaseHues.split(",");
        } else {
        	aBaseHues = new String[] { "70", "200" };
        }
        float[] baseHues = new float[aBaseHues.length];
        for(int i = 0; i < baseHues.length; ++i) {
        	baseHues[i] = Float.valueOf(aBaseHues[i]);
        }
    	Feed.setBaseHues(baseHues);

    	boolean firstLoad = settings.getBoolean("firstLoad", true);
        if (firstLoad) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Thank you for trying out Stanford Mobisocial's new software Musubi! Would you like to actively participate in our beta test? Press yes to receive e-mail updates about our progress.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try{
                            Uri.Builder b = new Uri.Builder();
                            b.scheme("http");
                            b.authority("suif.stanford.edu");
                            b.path("dungbeetle/emails.php");
                            Uri uri = b.build();

                            DefaultHttpClient client = new DefaultHttpClient();
                            HttpPost httpPost = new HttpPost(uri.toString());

                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

                                    
                            DBHelper helper = DBHelper.getGlobal(HomeActivity.this);
                            DBIdentityProvider ident = new DBIdentityProvider(helper);
                            nameValuePairs.add(new BasicNameValuePair("email", ident.userEmail()));
                            
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            client.execute(httpPost);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(HomeActivity.this, "Thank you for signing up!",
                                Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("firstLoad", false);
            editor.putString("ringtone", "content://media/internal/audio/media/14");
            editor.commit();
        }
        if (settings.getString("ringtone", null) == null) {

        	RingtoneManager ringtoneManager = new RingtoneManager(this);
            ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION);
            Cursor mCursor = ringtoneManager.getCursor();
            mCursor.moveToFirst();
            String ringtoneUri = null;
            while (!mCursor.isAfterLast()) {
            	String ringtone = mCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            	if(ringtone.equalsIgnoreCase("Deneb")) {
            		ringtoneUri = mCursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + mCursor.getString(RingtoneManager.ID_COLUMN_INDEX); 
            	
            		break;
            	}
            	mCursor.moveToNext();
            }
            mCursor.close();

            SharedPreferences.Editor editor = settings.edit();
            if (ringtoneUri != null) {
            	editor.putString("ringtone", ringtoneUri);
            }
            else {
            	Uri uri = ringtoneManager.getValidRingtoneUri(this);
            	
            	if(uri != null) {
            		editor.putString("ringtone", uri.toString());
            	}
            	else {
            		editor.putString("ringtone", "none");
            	}
            }
            editor.commit();
        }
        
        

        mNfc = new Nfc(this);
        // TODO: Combine doHandleInput calls in onNewIntent.
        doHandleInput(getIntent().getData());
        mNfc.addNdefHandler(new NdefHandler() {
                public int handleNdef(final NdefMessage[] messages){
                    HomeActivity.this.runOnUiThread(new Runnable(){
                            public void run() {
                                Log.d(TAG, "Handling ndef uri: " + uriFromNdef(messages));
                                doHandleInput(uriFromNdef(messages));
                            }
                        });
                    return NDEF_CONSUME;
                }
            });

        mNfc.setOnTagWriteListener(new Nfc.OnTagWriteListener(){
            public void onTagWrite(final int status){
                HomeActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        if (status == WRITE_OK) {
                            Toast.makeText(HomeActivity.this, "Wrote successfully!",
                                    Toast.LENGTH_SHORT).show();
                        } else if(status == WRITE_ERROR_READ_ONLY) {
                            Toast.makeText(HomeActivity.this, "Can't write read-only tag!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "Failed to write!",
                                    Toast.LENGTH_SHORT).show();
                        }
                        pushContactInfoViaNfc();
                    }
                }); 
            }
        });

        pushContactInfoViaNfc();
        /* sample code for demonstration of the nearby functionality without
         * a real hookup to the service.
         */
//        DBHelper helper = DBHelper.getGlobal(HomeActivity.this);
//        Map<byte[], byte[]> pkss = helper.getPublicKeySharedSecretMap();
//
//        Set<byte[]> ks = pkss.keySet();
//    	Iterator<byte[]> j = ks.iterator();
//        HashSet<byte[]> hks = new HashSet<byte[]>();
//        for(int i = 0; i < ks.size() / 2; ++i) {
//        	hks.add(j.next());
//        }
//		helper.updateNearby(hks);
//		for(byte[] k : ks) {
//			Contact c = helper.getContactForPublicKey(k);
//			helper.setNearby(k, !c.nearby);
//		}
//		helper.close();
    }

    public Uri uriFromNdef(NdefMessage... messages) {
        if (messages.length == 0 ||  messages[0].getRecords().length == 0) {
            return null;
        }

        try {
            return NdefFactory.parseUri(messages[0].getRecords()[0]);
        } catch (FormatException e) {
            return null;
        }
    }

    protected void doHandleInput(Uri uri) {
        if (DBG) Log.d(TAG, "Handling input uri " + uri);
        if (uri == null) {
            return;
        }

        if (uri.getScheme() == null) {
            Log.w(TAG, "Null uri scheme for " + uri);
        }
        
        if(uri.getScheme().equals(SHARE_SCHEME)
                || uri.getSchemeSpecificPart().startsWith(FriendRequest.PREFIX_JOIN)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setClass(this, HandleNfcContact.class);
            startActivity(intent);
        } else if(uri.getScheme().equals(GROUP_SESSION_SCHEME)) {
            Intent intent = new Intent().setClass(this, HandleGroupSessionActivity.class);
            intent.setData(uri);
            startActivity(intent);
        } else if (uri.getScheme().equals("content")) {
            if (uri.getAuthority().equals("vnd.mobisocial.db")) {
                if (uri.getPath().startsWith("/feed")) {
                    Intent view = new Intent(Intent.ACTION_VIEW, uri);
                    view.addCategory(Intent.CATEGORY_DEFAULT);
                    // TODO: fix in AndroidManifest.
                    //view.setClass(this, FeedActivity.class);
                    view.setClass(this, FeedHomeActivity.class);
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

    public void writeGroupToTag(Uri uri) {
        NdefRecord urlRecord = new NdefRecord(
                NdefRecord.TNF_ABSOLUTE_URI, 
                NdefRecord.RTD_URI, new byte[] {},
                uri.toString().getBytes());
        NdefMessage ndef = new NdefMessage(new NdefRecord[] { urlRecord });
        mNfc.enableTagWriteMode(ndef);
        Toast.makeText(this, "Touch a tag to write the group...", 
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

    public void pushGroupInfoViaNfc(Uri uri) {
        mNfc.share(NdefFactory.fromUri(uri));
    }

    public void pushContactInfoViaNfc() {
    	Uri uri = FriendRequest.getInvitationUri(this);
    	if (DBG) Log.w(TAG, "pushing " + uri.toString());
        mNfc.share(NdefFactory.fromUri(uri));
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
                FriendRequest.acceptFriendRequest(this, getIntent().getData(), false);
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

    @Override
    public void onResume() {
        super.onResume();
        mNfc.onResume(this);
        pushContactInfoViaNfc();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) {
            return;
        }
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * Handle the click of a Feature button.
     * 
     * @param v View
     * @return void
     */

    public void onClickFeature(View v) {
        int id = v.getId();

        Intent intent;
        switch (id) {
            case R.id.home_btn_latest:
                intent = new Intent().setClass(getApplicationContext(), FeedListActivity.class);
                startActivity(intent);
                break;
            case R.id.home_btn_friends:
                intent = new Intent().setClass(getApplicationContext(), ContactsActivity.class);
                startActivity(intent);
                break;
            case R.id.home_btn_profile:
                intent = new Intent().setClass(getApplicationContext(), ViewContactActivity.class);
                intent.putExtra("contact_id", Contact.MY_ID);
                startActivity(intent);
                break;
            case R.id.home_btn_groups:
                intent = new Intent().setClass(getApplicationContext(), GroupsActivity.class);
                startActivity(intent);
                break;
            case R.id.home_btn_new_group:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Enter group name:");
                final EditText input = new EditText(this);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String groupName = input.getText().toString();
                        Group g;
                        if (groupName.length() > 0) {
                            g = Group.create(HomeActivity.this, groupName, mHelper);
                        } else {
                            g = Group.create(HomeActivity.this);
                        }

                        Helpers.sendToFeed(HomeActivity.this,
                                StatusObj.from("Welcome to " + g.name + "!"),
                                Feed.uriForName(g.feedName));

                        Intent launch = new Intent(Intent.ACTION_VIEW);
                        launch.setDataAndType(Feed.uriForName(g.feedName), Feed.MIME_TYPE);
                        startActivity(launch);
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
                // intent = new Intent().setClass(getApplicationContext(),
                // NewGroupActivity.class);
                // startActivity (intent);
                break;
            case R.id.home_btn_settings:
                intent = new Intent().setClass(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.home_btn_nearby:
                Intent launch = new Intent();
                launch.setClass(this, NearbyGroupsActivity.class);
                startActivity(launch);
                break;
            default:
                break;
        }
    }
}