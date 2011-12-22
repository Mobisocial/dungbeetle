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

package edu.stanford.mobisocial.dungbeetle.feed.action;

import java.io.IOException;

import mobisocial.socialkit.Obj;

import org.mobisocial.corral.ContentCorral;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

/**
 * Captures an image to share with a feed.
 *
 */
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
                    public void onResult(Uri imageUri, byte[] data) {
                        Obj obj;
                        Uri storedUri = ContentCorral.storeContent(context, imageUri, "image/jpeg");
                        try {
                            obj = PictureObj.from(context, storedUri);
                        } catch (IOException e) {
                            Log.w("CameraAction", "failed to capture image", e);
                            obj = PictureObj.from(data);
                        }
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
