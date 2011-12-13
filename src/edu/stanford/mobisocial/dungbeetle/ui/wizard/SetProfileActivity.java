package edu.stanford.mobisocial.dungbeetle.ui.wizard;
import mobisocial.nfc.Nfc;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.ViewContactActivity;

public class SetProfileActivity extends MusubiBaseActivity {
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

        setContentView(R.layout.activity_home);

        ImageView latestButton = (ImageView) findViewById(R.id.home_btn_latest);
        ImageView friendsButton = (ImageView) findViewById(R.id.home_btn_friends);
        ImageView groupsButton = (ImageView) findViewById(R.id.home_btn_groups);
        ImageView newGroupButton = (ImageView) findViewById(R.id.home_btn_new_group);
        ImageView nearbyButton = (ImageView) findViewById(R.id.home_btn_nearby);
        ImageView settingsButton = (ImageView) findViewById(R.id.home_btn_settings);

        latestButton.setAlpha(50);
        friendsButton.setAlpha(50);
        groupsButton.setAlpha(50);
        newGroupButton.setAlpha(50);
        nearbyButton.setAlpha(50);
        settingsButton.setAlpha(50);
        
        MusubiBaseActivity.doTitleBar(this);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("First, let's set up your profile. Click on the profile button to continue.")
               .setCancelable(false)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();

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
            /*case R.id.home_btn_latest:
                intent = new Intent().setClass(getApplicationContext(), FeedListActivity.class);
                startActivity(intent);
                break;
            case R.id.home_btn_friends:
                intent = new Intent().setClass(getApplicationContext(), ContactsActivity.class);
                startActivity(intent);
                break;*/
            case R.id.home_btn_profile:
                intent = new Intent().setClass(getApplicationContext(), ChangePictureActivity.class);
                intent.putExtra("contact_id", Contact.MY_ID);
                startActivity(intent);
                break;
            /*case R.id.home_btn_groups:
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
                            g = Group.create(SetProfileActivity.this, groupName, mHelper);
                        } else {
                            g = Group.create(SetProfileActivity.this);
                        }

                        Helpers.sendToFeed(SetProfileActivity.this,
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
                launch.setClass(this, NearbyActivity.class);
                startActivity(launch);
                break;*/
            default:
                break;
        }
    }
}