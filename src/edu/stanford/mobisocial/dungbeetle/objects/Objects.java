package edu.stanford.mobisocial.dungbeetle.objects;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public final class Objects {

    // Basic property names for all objects
    public static final String TYPE = "type";
    public static final String FEED_NAME = "feedName";
    public static final String SEQUENCE_ID = "sequenceId";
    public static final String TIMESTAMP = "timestamp";
    public static final String APP_ID = "appId";

    private static final List<Object> objs = new ArrayList<Object>();

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
    }

	public static FeedRenderer getFeedRenderer(JSONObject json) {
        for(Object obj : objs){
            if(obj instanceof FeedRenderer){
                FeedRenderer o = (FeedRenderer)obj;
                if(o.willRender(json)){
                    return o;
                }
            }
        }
        return null;
	}

	public static Activator getActivator(JSONObject json) {
        for(Object obj : objs){
            if(obj instanceof Activator){
                Activator o = (Activator)obj;
                if(o.willActivate(json)){
                    return o;
                }
            }
        }
        return null;
	}


	public static IncomingMessageHandler 
        getIncomingMessageHandler(Contact c, JSONObject json) {
        for(Object obj : objs){
            if(obj instanceof IncomingMessageHandler){
                IncomingMessageHandler o = (IncomingMessageHandler)obj;
                if(o.willHandle(c, json)){
                    return o;
                }
            }
        }
        return null;
	}



}
