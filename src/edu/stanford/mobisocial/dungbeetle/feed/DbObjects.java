package edu.stanford.mobisocial.dungbeetle.feed;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ActivityPullObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedRefObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FileObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FriendAcceptObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.IMObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToGroupObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToWebSessionObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.JoinNotificationObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.MusicObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PhoneStateObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.SubscribeReqObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public final class DbObjects {

    // Basic property names for all objects
    public static final String TYPE = "type";
    public static final String FEED_NAME = "feedName";
    public static final String SEQUENCE_ID = "sequenceId";
    public static final String TIMESTAMP = "timestamp";
    public static final String APP_ID = "appId";

    private static final List<DbEntryHandler> objs = new ArrayList<DbEntryHandler>();

    static{
		objs.add(new AppReferenceObj());
		objs.add(new SubscribeReqObj());
		objs.add(new IMObj());
		objs.add(new InviteToWebSessionObj());
		objs.add(new AppReferenceObj());
        objs.add(new InviteToSharedAppFeedObj());
        objs.add(new InviteToGroupObj());
        objs.add(new FileObj());
        objs.add(new ProfileObj());
        objs.add(new PresenceObj());
        objs.add(new StatusObj());
        objs.add(new LocationObj());
        objs.add(new ProfilePictureObj());
        objs.add(new PictureObj());
        objs.add(new VoiceObj());
        objs.add(new ActivityPullObj());
        objs.add(new FeedRefObj());
        objs.add(new JoinNotificationObj());
        objs.add(new FriendAcceptObj());
        objs.add(new PhoneStateObj());
        objs.add(new MusicObj());
        objs.add(new FeedAnchorObj());
    }

	public static FeedRenderer getFeedRenderer(JSONObject json) {
	    String type = json.optString("type");
	    for (DbEntryHandler obj : objs) {
            if (obj instanceof FeedRenderer && obj.getType().equals(type)) {
                return (FeedRenderer)obj;
            }
        }
        return null;
	}

	public static Activator getActivator(JSONObject json) {
	    String type = json.optString("type");
        for (DbEntryHandler obj : objs) {
            if (obj instanceof Activator && obj.getType().equals(type)) {
                return (Activator)obj;
            }
        }
        return null;
	}

	public static DbEntryHandler getIncomingMessageHandler(JSONObject json) {
	    String type = json.optString("type");
        for (DbEntryHandler obj : objs) {
            if (obj instanceof DbEntryHandler) {
                DbEntryHandler o = (DbEntryHandler)obj;
                if (o.getType().equals(type)) {
                    return o;
                }
            }
        }
        return null;
	}

	public static String[] getRenderableTypes() {
	    List<String> renderables = new ArrayList<String>();
	    for (DbEntryHandler o : objs) {
	        if (o instanceof FeedRenderer){
	            renderables.add(o.getType());
	        }
	    }
	    return renderables.toArray(new String[renderables.size()]);
	}
}
