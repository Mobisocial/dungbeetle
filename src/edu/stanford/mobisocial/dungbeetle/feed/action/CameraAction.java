package edu.stanford.mobisocial.dungbeetle.feed.action;

import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class CameraAction implements FeedAction {

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        ((InstrumentedActivity)context).doActivityForResult(new PhotoTaker(
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
