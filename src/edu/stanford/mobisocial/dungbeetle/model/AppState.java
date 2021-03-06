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

package edu.stanford.mobisocial.dungbeetle.model;

import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;

import edu.stanford.mobisocial.dungbeetle.feed.objects.AppStateObj;

public class AppState extends DbObject {
    public static final String EXTRA_APPLICATION_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";
    public static final String EXTRA_APPLICATION_PACKAGE = "mobisocial.db.PACKAGE";
    public static final String EXTRA_APPLICATION_STATE = "mobisocial.db.STATE";
    public static final String EXTRA_APPLICATION_IMG = "mobisocial.db.THUMBNAIL_IMAGE";
    public static final String EXTRA_APPLICATION_TEXT = "mobisocial.db.THUMBNAIL_TEXT";
    public static final String EXTRA_FEED_URI = Musubi.EXTRA_FEED_URI;
    public static final String EXTRA_OBJ_HASH = "mobisocial.db.OBJ_HASH";

    public AppState(JSONObject json) {
        super(AppStateObj.TYPE, json);
    }

    public AppState(DbObj obj) {
        super(obj.getType(), obj.getJson());
    }

    @Deprecated
    public static AppState fromIntent(Intent intent) {
        String arg = intent.getStringExtra(EXTRA_APPLICATION_ARGUMENT);
        String pkg = intent.getStringExtra(EXTRA_APPLICATION_PACKAGE);
        String state = intent.getStringExtra(EXTRA_APPLICATION_STATE);
        String thumbImg = intent.getStringExtra(EXTRA_APPLICATION_IMG);
        String thumbText = intent.getStringExtra(EXTRA_APPLICATION_TEXT);
        Uri feedUri = (Uri)intent.getParcelableExtra(EXTRA_FEED_URI);
        return new AppState(pkg, arg, state, thumbImg, thumbText, feedUri.getLastPathSegment(), null);
    }

    public String pkg() {
        return this.mJson.optString("packageName");
    }

    public AppState(String pkg, String arg, String feedName, String groupUri) {
        super(AppStateObj.TYPE, AppStateObj.json(pkg, arg, feedName, groupUri));
    }

    public AppState(String pkg, String arg, String state, String b64JpgThumbnail,
            String thumbText, String feedName, String  groupUri) {
        super(AppStateObj.TYPE, AppStateObj.json(
                pkg, arg, state, b64JpgThumbnail, thumbText, feedName, groupUri));
    }

    public String getThumbnailImage() {
        if (mJson.has(AppStateObj.THUMB_JPG)) {
            return mJson.optString(AppStateObj.THUMB_JPG);
        }
        return null;
    }

    public String getThumbnailText() {
        if (mJson.has(AppStateObj.THUMB_TEXT)) {
            return mJson.optString(AppStateObj.THUMB_TEXT);
        }
        return null;
    }

    public String getThumbnailHtml() {
        if (mJson.has(AppStateObj.THUMB_HTML)) {
            return mJson.optString(AppStateObj.THUMB_HTML);
        }
        return null;
    }
}
