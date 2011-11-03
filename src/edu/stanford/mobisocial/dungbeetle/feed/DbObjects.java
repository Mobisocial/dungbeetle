package edu.stanford.mobisocial.dungbeetle.feed;
import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.MemObj;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.AppStateObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.DeleteObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedAnchorObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedRefObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FriendAcceptObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.IMObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToGroupObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToSharedAppFeedObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.InviteToWebSessionObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.JoinNotificationObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LikeObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LinkObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.MusicObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PhoneStateObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.RemoteIntentObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.SubscribeReqObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.UnknownObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VideoObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public final class DbObjects {

    // Basic property names for all objects
    public static final String TYPE = "type";
    public static final String FEED_NAME = "feedName";
    public static final String SEQUENCE_ID = "sequenceId";
    public static final String TIMESTAMP = "timestamp";
    public static final String APP_ID = "appId";

    /**
     * {@see DbRelation}
     */
    public static final String TARGET_HASH = "target_hash";
    public static final String TARGET_RELATION = "target_relation";

    private static final List<DbEntryHandler> objs = new ArrayList<DbEntryHandler>();
    private static UnknownObj mUnknownObjHandler = new UnknownObj();
    static {
        objs.add(new AppObj());
        objs.add(new AppStateObj());
        objs.add(new AppReferenceObj());
		objs.add(new SubscribeReqObj());
		objs.add(new IMObj());
		objs.add(new InviteToWebSessionObj());
        objs.add(new InviteToSharedAppFeedObj());
        objs.add(new InviteToGroupObj());
        objs.add(new LinkObj());
        objs.add(new ProfileObj());
        objs.add(new PresenceObj());
        objs.add(new StatusObj());
        objs.add(new LocationObj());
        objs.add(new ProfilePictureObj());
        objs.add(new PictureObj());
        objs.add(new VideoObj());
        objs.add(new VoiceObj());
        objs.add(new FeedRefObj());
        objs.add(new JoinNotificationObj());
        objs.add(new FriendAcceptObj());
        objs.add(new PhoneStateObj());
        objs.add(new MusicObj());
        objs.add(new FeedAnchorObj());
        objs.add(new DeleteObj());
        objs.add(new LikeObj());
        objs.add(new RemoteIntentObj());
    }

	public static FeedRenderer getFeedRenderer(String type) {
	    for (DbEntryHandler obj : objs) {
            if (obj instanceof FeedRenderer && obj.getType().equals(type)) {
                return (FeedRenderer)obj;
            }
        }
        return null;
	}

	public static Activator getActivator(String type) {
        for (DbEntryHandler obj : objs) {
            if (obj instanceof Activator && obj.getType().equals(type)) {
                return (Activator)obj;
            }
        }
        return null;
	}

	public static DbEntryHandler getObjHandler(JSONObject json) {
	    String type = json.optString("type");
	    return forType(type);
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

	public static DbEntryHandler forType(String requestedType) {
	    if (requestedType == null) {
	        return null;
	    }
        for (DbEntryHandler type : objs) {
            if (type.getType().equals(requestedType)) {
                return type;
            }
        }
        return mUnknownObjHandler;
    };

    public static String getFeedObjectClause(String[] types) {
    	if(types == null) {
    		types = DbObjects.getRenderableTypes();
    	}
        StringBuffer allowed = new StringBuffer();
        for (String type : types) {
            allowed.append(",'").append(type).append("'");
        }
        Log.w("DbObjects", DbObject.TYPE + " in (" + allowed.substring(1) + ")");
        return DbObject.TYPE + " in (" + allowed.substring(1) + ")";
    }

    /**
     * We used to encode binary content as base64 as a json field. Now,
     * an Obj understands binary. This method converts an old json representation
     * to the new Obj format.
     */
    public static Obj convertOldJsonToObj(Context c, String type, JSONObject json) {
        DbEntryHandler e = forType(type);
        if (e instanceof UnprocessedMessageHandler) {
            Pair<JSONObject, byte[]> r = ((UnprocessedMessageHandler)e)
                    .handleUnprocessed(c, json);
            if (r != null) {
                return new MemObj(type, r.first, r.second);
            }
        }
        return new MemObj(type, json);
    }
}
