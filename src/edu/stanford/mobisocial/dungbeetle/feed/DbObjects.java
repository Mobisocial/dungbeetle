/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.feed;
import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONObject;

import android.content.Context;
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
import edu.stanford.mobisocial.dungbeetle.feed.objects.SharedSecretObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
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

    public static final String JSON_INT_KEY = "obj_intkey";
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
        objs.add(new SharedSecretObj()) ;
        objs.add(new DeleteObj());
        objs.add(new LikeObj());
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