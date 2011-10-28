package edu.stanford.mobisocial.dungbeetle.obj.handler;

import mobisocial.socialkit.musubi.DbObj;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.NoNotify;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.ui.FeedListActivity;
import edu.stanford.mobisocial.dungbeetle.ui.ViewContactActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class NotificationObjHandler extends ObjHandler {
    String TAG = "NotificationObjHandler";
    final DBHelper mHelper;
    public NotificationObjHandler(DBHelper helper) {
        mHelper = helper;
    }

    @Override
    public void handleObj(Context context, DbEntryHandler typeInfo, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        long senderId = obj.getSender().getLocalId();
        if (senderId == Contact.MY_ID) {
            return;
        }

        if (typeInfo == null || !(typeInfo instanceof FeedRenderer)) {
            return;
        }

        if (typeInfo instanceof NoNotify) {
            return;
        }
        
        switch(Feed.typeOf(feedUri)) {
        	case Feed.FEED_FRIEND: {
        	    Intent launch = new Intent().setClass(context, ViewContactActivity.class);
                launch.putExtra("contact_id", senderId);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                        launch, PendingIntent.FLAG_CANCEL_CURRENT);
                (new PresenceAwareNotify(context)).notify("New Musubi message",
                        "New Musubi message", "From " + obj.getSender().getName(),
                        contentIntent);
        		break;
        	}
        	case Feed.FEED_GROUP: {
                String feedName = feedUri.getLastPathSegment();
                Maybe<Group> group = mHelper.groupForFeedName(feedName);
                Intent launch = new Intent(Intent.ACTION_VIEW);
                launch.setClass(context, FeedListActivity.class);
                if(Build.VERSION.SDK_INT < 11)
                	launch.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
            	else 
            		launch.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                        launch, PendingIntent.FLAG_CANCEL_CURRENT);
        
                try {
                    (new PresenceAwareNotify(context)).notify("New Musubi message",
                            "New Musubi message", "In " + ((Group) group.get()).name,
                            contentIntent);
                } catch (NoValError e) {
                    Log.e(TAG, "No group while notifying for " + feedName);
                }
        		break;
        	}
        	case Feed.FEED_RELATED: {
        		throw new RuntimeException("never should get a related feed from the network");
        	}
        }
    }
}
