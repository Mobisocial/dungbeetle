package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;


public class HandleGroupSessionActivity extends Activity {
	private TextView mNameText;
    public static final String TAG = "HandleGroupSessionActivity";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_group);
		Intent intent = getIntent();
		final String scheme=intent.getScheme();
		if (!HomeActivity.GROUP_SESSION_SCHEME.equals(scheme)) {
		    Toast.makeText(this, "Failed to receive url :(", Toast.LENGTH_SHORT).show();
		    return;
		}

		final Uri uri = intent.getData();
		if(uri != null) {
            GroupProviders.GroupProvider gp1 = GroupProviders.forUri(uri);
            String feedName = gp1.feedName(uri);
            DBHelper helper = DBHelper.getGlobal(this);
            Group g = helper.groupForFeedName(feedName);
            long id = -1;
            if(g == null) {
                // group does not exist yet, time to prompt for join
                Log.i(TAG, "Read uri: " + uri);
                GroupProviders.GroupProvider gp = GroupProviders.forUri(uri);
                String groupName = gp.groupName(uri);
                mNameText = (TextView)findViewById(R.id.text);
                mNameText.setText("Would you like to join the group '" + groupName + "'?");
                Button b1 = (Button)findViewById(R.id.yes_button);
                b1.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Group.join(HandleGroupSessionActivity.this, uri);
                            finish();
                        }
                    });
                Button b2 = (Button)findViewById(R.id.no_button);
                b2.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            finish();
                        }
                    });
            } else {
                // group exists already, load view
                id = g.id;

                Group.view(HandleGroupSessionActivity.this, g);
                finish();
            }
		} else {
			Toast.makeText(this, "Received null url...", Toast.LENGTH_SHORT).show();
		}
	}
}