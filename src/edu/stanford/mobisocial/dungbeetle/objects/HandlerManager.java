package edu.stanford.mobisocial.dungbeetle.objects;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;

public final class HandlerManager {
	public static List<MessageHandler> getDefaults(Context context) {
		List<MessageHandler> receivers = new ArrayList<MessageHandler>();
		receivers.add(new InviteToSharedAppHandler(context));
		receivers.add(new SubscribeReqHandler(context));
		receivers.add(new IMHandler(context));
		receivers.add(new InviteToWebSessionHandler(context));
		receivers.add(new InviteToSharedAppHandler(context));
        receivers.add(new InviteToSharedAppFeedHandler(context));
        receivers.add(new InviteToGroupHandler(context));
        receivers.add(new SendFileHandler(context));
        receivers.add(new ProfileHandler(context));
        receivers.add(new PresenceHandler(context));
        receivers.add(new StatusHandler(context));
		return receivers;
	}
}
