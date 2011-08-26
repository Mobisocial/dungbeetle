package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.FeedViewActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;

public class CameraAction implements FeedAction {

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        RichListActivity.doActivityForResult((Activity)context, 
            new PhotoTaker(
                context, 
                new PhotoTaker.ResultHandler() {
                    @Override
                    public void onResult(byte[] data) {
                        DbObject obj = PictureObj.from(data);
                        Helpers.sendToFeed(
                            context, obj, feedUri);
                    }
                }, 200, true));
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
