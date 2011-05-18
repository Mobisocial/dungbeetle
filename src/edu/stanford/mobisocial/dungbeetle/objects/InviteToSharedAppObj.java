package edu.stanford.mobisocial.dungbeetle.objects;
import android.view.Gravity;
import android.view.ViewGroup;

import java.util.List;
import org.json.JSONException;

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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.stanford.mobisocial.dungbeetle.model.Contact;


public class InviteToSharedAppObj implements IncomingMessageHandler, FeedRenderer {
	private static final String TAG = "InviteToSharedAppObj";

    public static final String TYPE = "invite_app_session";
    public static final String ARG = "arg";
    public static final String PACKAGE_NAME = "packageName";
    public static final String PARTICIPANTS = "participants";
    public static final String FEED_NAME = "feedName";

    public static JSONObject json(String packageName, String arg){
        JSONObject obj = new JSONObject();
        try{
            obj.put(PACKAGE_NAME, packageName);
            obj.put(ARG, arg);
        }catch(JSONException e){}
        return obj;
    }

    public boolean willHandle(Contact from, JSONObject msg){ 
        return msg.optString("type").equals(TYPE);
    }
    public void handleReceived(Context context, Contact from, JSONObject obj){
        String packageName = obj.optString(PACKAGE_NAME);
        String arg = obj.optString(ARG);
        Log.i(TAG, "Received invite with arg: " + arg);
        Intent launch = new Intent();
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", arg);
        launch.putExtra("creator", false);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.setPackage(packageName);
        final PackageManager mgr = context.getPackageManager();
        List<ResolveInfo> resolved = mgr.queryIntentActivities(launch, 0);
        if (resolved == null || resolved.size() == 0) {
            Toast.makeText(context, 
                           "Could not find application to handle invite", 
                           Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityInfo info = resolved.get(0).activityInfo;
        launch.setComponent(new ComponentName(
                                info.packageName,
                                info.name));
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_CANCEL_CURRENT);

        (new PresenceAwareNotify(context)).notify(
            "New Invitation",
            "Invitation received from " + from.name, 
            "Click to launch application.", 
            contentIntent);
    }
    
	public boolean willRender(JSONObject object) { return true; }

	public void render(final Context context, final ViewGroup frame, final JSONObject content) {
        TextView valueTV = new TextView(context);
        valueTV.setText(content.optString(ARG));
        valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
        frame.addView(valueTV);
        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.setComponent(
                        new ComponentName("edu.stanford.junction.sample.jxwhiteboard",
                        "edu.stanford.junction.sample.jxwhiteboard.JXWhiteboardActivity"));
                launch.putExtra("android.intent.extra.APPLICATION_ARGUMENT", content.optString(ARG));
                context.startActivity(launch);
            }
        });
    }

}