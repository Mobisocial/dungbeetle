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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;

/**
 * Adds a PictureObj to a feed from an external Android application
 * such as the Gallery.
 *
 */
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
                            // TODO: mimeType; local_uri = data.toString();
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
