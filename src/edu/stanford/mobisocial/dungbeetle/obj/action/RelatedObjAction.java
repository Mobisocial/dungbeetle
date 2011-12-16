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


package edu.stanford.mobisocial.dungbeetle.obj.action;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

/**
 * Opens the given Obj using its {@link Activator}.
 *
 */
public class RelatedObjAction extends ObjAction {

    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        Uri feedUri = obj.getContainingFeed().getUri();
        long hash = obj.getHash();
        // TODO:
        /*Intent viewComments = new Intent(Intent.ACTION_VIEW);
        viewComments.setDataAndType(objUri, DbObject.MIME_TYPE);
        mmContext.startActivity(viewComments);*/
        Uri objUri = feedUri.buildUpon().encodedPath(feedUri.getPath() + ":" + hash).build();

        Intent objViewActivity = new Intent(Intent.ACTION_VIEW);
        objViewActivity.setDataAndType(objUri, Feed.MIME_TYPE);
        context.startActivity(objViewActivity);
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        return MusubiBaseActivity.isDeveloperModeEnabled(context);
    }

    @Override
    public String getLabel(Context context) {
        return "See Related";
    }
}
