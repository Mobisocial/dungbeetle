package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.VoiceRecorderActivity;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.FeedViewActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RichListActivity;

public class GalleryAction implements FeedAction {

    @Override
    public String getName() {
        return "Gallery";
    }

    @Override
    public void onClick(final Context context, final Uri feedUri) {
        ((InstrumentedActivity)context).doActivityForResult(new GalleryCallout(context, feedUri));
    }

    @Override
    public boolean isActive() {
        return true;
    }

    class GalleryCallout implements ActivityCallout {
        private final Context mmContext;
        private final Uri mmFeedUri;

        private GalleryCallout(Context context, Uri feedUri) {
            mmContext = context;
            mmFeedUri = feedUri;
        }

        @Override
        public void handleResult(int resultCode, final Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            DbObject outboundObj = PictureObj.from(mmContext, data.getData());
                            Helpers.sendToFeed(mmContext, outboundObj, mmFeedUri);
                        } catch (IOException e) {
                            Toast.makeText(mmContext, "Error reading photo data.", Toast.LENGTH_SHORT).show();
                            Log.e(HomeActivity.TAG, "Error reading photo data.", e);
                        }
                    }
                }.start();
            }
        }

        @Override
        public Intent getStartIntent() {
            Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
            gallery.setType("image/*");
            return Intent.createChooser(gallery, null);
        }
    };
}
