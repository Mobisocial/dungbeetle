package edu.stanford.mobisocial.dungbeetle.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.FeedActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.actions.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.model.AppReference;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.objects.AppReferenceObj;

public class LaunchApplicationAction implements FeedAction {

    @Override
    public String getName() {
        return "Application...";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        AppReferenceObj.promptForApplication(context, new AppReferenceObj.Callback() {
            @Override
            public void onAppSelected(String pkg, String arg, Intent localLaunch) {
                DbObject obj = new AppReference(pkg, arg);
                Helpers.sendToFeed(context, obj, feedUri);
                localLaunch.putExtra("mobisocial.db.FEED", feedUri);
                localLaunch.putExtra(AppReference.EXTRA_APPLICATION_ARGUMENT, arg);
                localLaunch.putExtra("mobisocial.db.PACKAGE", pkg);
                context.startActivity(localLaunch);
            }
        });
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
