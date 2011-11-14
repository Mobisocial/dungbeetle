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
import edu.stanford.mobisocial.dungbeetle.model.Group.GroupParameters;
import edu.stanford.mobisocial.dungbeetle.model.Group.InvalidGroupUri;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.Util;


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
		if(uri == null) {
			Toast.makeText(this, "Received null url...", Toast.LENGTH_SHORT).show();
			return;
		}
		GroupParameters gp;
		try {
			gp = Group.getGroupParameters(uri);
		} catch (InvalidGroupUri e) {
			Toast.makeText(this, "Received invalid group url...", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "invalid group url received", e);
			return;
		}
        DBHelper helper = DBHelper.getGlobal(this);
        Group g = helper.groupForFeedName(Util.SHA1(gp.name.getEncoded()));
        if(g != null) {
            // group exists already, load view
            Group.view(HandleGroupSessionActivity.this, g);
            finish();
            return;
        }
        // group does not exist yet, time to prompt for join
        Log.i(TAG, "Read uri: " + uri);
        String groupName = gp.human;
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
	}
}