package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.List;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.objects.InviteObj;


class InviteToSharedAppHandler extends MessageHandler implements Renderable {
	private Context mContext;
	
	public InviteToSharedAppHandler(Context context) {
		super(context);
		mContext = context;
	}

	private static final String TAG = "messagehandler";
    public boolean willHandle(Contact from, JSONObject msg){ 
        return msg.optString("type").equals("invite_app_session");
    }
    public void handleReceived(Contact from, JSONObject obj){
        String packageName = obj.optString(InviteObj.PACKAGE_NAME);
        String arg = obj.optString(InviteObj.ARG);
        Log.i(TAG, "Received invite with arg: " + arg);
        Intent launch = new Intent();
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
        launch.putExtra("creator", false);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.setPackage(packageName);
        final PackageManager mgr = mContext.getPackageManager();
        List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
        if (resolved == null || resolved.size() == 0) {
            Toast.makeText(mContext, 
                           "Could not find application to handle invite", 
                           Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityInfo info = resolved.get(0).activityInfo;
        launch.setComponent(new ComponentName(
                                info.packageName,
                                info.name));
        PendingIntent contentIntent = PendingIntent.getActivity(
            mContext, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

        getPresenceAwareNotify().notify(
            "New Invitation",
            "Invitation received from " + from.name, 
            "Click to launch application.", 
            contentIntent);
    }
    
    public void renderToFeed(View frame, JSONObject content) {
        TextView bodyText = (TextView)frame.findViewById(R.id.body_text);
        bodyText.setText("Invitation to an app");
    }
}