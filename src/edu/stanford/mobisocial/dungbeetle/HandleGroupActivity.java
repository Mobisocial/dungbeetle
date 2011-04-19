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
import java.util.ArrayList;
import java.util.List;


/**
 * Handle the acquisition of a group uri. This uri represents a private group on dungbeetle.
 */
public class HandleGroupActivity extends Activity {
	private TextView mNameText;
    public static final String TAG = "HandleGroupActivity";
	private NotificationManager mNotificationManager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handle_group);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Intent intent = getIntent();

        final long contactId = intent.getLongExtra("sender", -1);
        final String groupName = intent.getStringExtra("groupName");
        final String feedName = intent.getStringExtra("sharedFeedName");
        final long[] participants = intent.getLongArrayExtra("participants");


        mNameText = (TextView)findViewById(R.id.text);
        mNameText.setText("Would you like to join the group '" + groupName + 
                          "' with " + (participants.length - 1) + " others?");
        Button b1 = (Button)findViewById(R.id.yes_button);
        b1.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {

                    Helpers.addGroupFromInvite(
                        HandleGroupActivity.this, 
                        groupName,
                        feedName,
                        contactId,
                        participants);


                    // Notify group created
                    Intent launch = new Intent();
                    launch.setAction(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setComponent(
                        new ComponentName(
                            getPackageName(),
                            DungBeetleActivity.class.getName()));
                    Notification notification = new Notification(
                        R.drawable.icon, "Added new group.", System.currentTimeMillis());
                    PendingIntent contentIntent = PendingIntent.getActivity(
                        HandleGroupActivity.this, 0, launch, 0);
                    notification.setLatestEventInfo(
                        HandleGroupActivity.this, "New Group",
                        "Click to view.", 
                        contentIntent);
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    mNotificationManager.notify(0, notification);


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



