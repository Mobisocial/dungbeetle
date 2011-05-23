package edu.stanford.mobisocial.dungbeetle.model;
import edu.stanford.mobisocial.dungbeetle.objects.ActivityPullObj;
import edu.stanford.mobisocial.dungbeetle.objects.IMObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToGroupObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppObj;
import edu.stanford.mobisocial.dungbeetle.objects.InviteToWebSessionObj;
import edu.stanford.mobisocial.dungbeetle.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.objects.SendFileObj;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.objects.SubscribeReqObj;
import edu.stanford.mobisocial.dungbeetle.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.objects.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.objects.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.objects.iface.DbEntryHandler;

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
		objs.add(new InviteToSharedAppObj());
		objs.add(new SubscribeReqObj());
		objs.add(new IMObj());
		objs.add(new InviteToWebSessionObj());
		objs.add(new InviteToSharedAppObj());
        objs.add(new InviteToSharedAppFeedObj());
        objs.add(new InviteToGroupObj());
        objs.add(new SendFileObj());
        objs.add(new ProfileObj());
        objs.add(new PresenceObj());
        objs.add(new StatusObj());
        objs.add(new ProfilePictureObj());
        objs.add(new PictureObj());
        objs.add(new VoiceObj());
        objs.add(new ActivityPullObj());
    }

	public static FeedRenderer getFeedRenderer(JSONObject json) {
	    for (DbEntryHandler obj : objs) {
            if (obj instanceof FeedRenderer && obj.getType().equals(json.optString("type"))) {
                return (FeedRenderer)obj;
            }
        }
        return null;
	}

	public static Activator getActivator(JSONObject json) {
        for (DbEntryHandler obj : objs) {
            if (obj instanceof Activator && obj.getType().equals(json.optString("type"))) {
                return (Activator)obj;
            }
        }
        return null;
	}

	public static DbEntryHandler getIncomingMessageHandler(Contact c, JSONObject json) {
        for(DbEntryHandler obj : objs){
            if(obj instanceof DbEntryHandler) {
                DbEntryHandler o = (DbEntryHandler)obj;
                if (o.getType().equals(json.optString("type"))) {
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
