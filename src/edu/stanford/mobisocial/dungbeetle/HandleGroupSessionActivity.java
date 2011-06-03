package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;


public class HandleGroupSessionActivity extends Activity {
	private TextView mNameText;
    public static final String TAG = "HandleGroupSessionActivity";
	private NotificationManager mNotificationManager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_group);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Intent intent = getIntent();
		final String scheme=intent.getScheme();
		if(scheme != null && scheme.equals(DungBeetleActivity.GROUP_SESSION_SCHEME)){
			final Uri uri = intent.getData();
			if(uri != null){
                Log.i(TAG, "Read uri: " + uri);
                GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
                String groupName = gp.groupName(uri);
                mNameText = (TextView)findViewById(R.id.text);
                mNameText.setText("Would you like to join the group '" + groupName + "'?");
                Button b1 = (Button)findViewById(R.id.yes_button);
                b1.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            loadGroup(uri);
                            finish();
                        }
                    });
                Button b2 = (Button)findViewById(R.id.no_button);
                b2.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            finish();
                        }
                    });
			}
			else{
				Toast.makeText(this, "Received null url...", Toast.LENGTH_SHORT).show();
			}
		}
		else{
			Toast.makeText(this, "Failed to receive url :(", Toast.LENGTH_SHORT).show();
		}
	}

    private void loadGroup(Uri uri){
        Uri gUri = Helpers.addDynamicGroup(this, uri);

        // Force an immediate update
        long id = Long.valueOf(gUri.getLastPathSegment());
        if(id > -1){
            GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
            DBHelper helper = new DBHelper(this);
            DBIdentityProvider ident = new DBIdentityProvider(helper);
            gp.forceUpdate(id, uri, this, ident);
            ident.close();
            helper.close();
        }
        try {
            Maybe<Group> group = Group.forId(this, id);
            Group.view(HandleGroupSessionActivity.this, group.get());
        } catch (NoValError e) {}
    }

}



