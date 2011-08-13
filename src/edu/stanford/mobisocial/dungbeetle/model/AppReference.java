package edu.stanford.mobisocial.dungbeetle.model;

import org.json.JSONObject;

import android.content.Intent;

import edu.stanford.mobisocial.dungbeetle.feed.objects.AppReferenceObj;

public class AppReference extends DbObject {
    public static final String EXTRA_APPLICATION_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";
    public static final String EXTRA_APPLICATION_PACKAGE = "mobisocial.db.PACKAGE";
    public static final String EXTRA_APPLICATION_STATE = "mobisocial.db.STATE";
    public static final String EXTRA_APPLICATION_IMG = "mobisocial.db.THUMBNAIL_IMAGE";
    public static final String EXTRA_APPLICATION_TEXT = "mobisocial.db.THUMBNAIL_TEXT";
    public AppReference(JSONObject json) {
        super(AppReferenceObj.TYPE, json);
    }

    public static AppReference fromIntent(Intent intent) {
        String arg = intent.getStringExtra(EXTRA_APPLICATION_ARGUMENT);
        String pkg = intent.getStringExtra(EXTRA_APPLICATION_PACKAGE);
        String state = intent.getStringExtra(EXTRA_APPLICATION_STATE);
        String thumbImg = intent.getStringExtra(EXTRA_APPLICATION_IMG);
        String thumbText = intent.getStringExtra(EXTRA_APPLICATION_TEXT);
        return new AppReference(pkg, arg, state, thumbImg, thumbText);
    }

    public String pkg() {
        return this.mJson.optString("packageName");
    }

    public AppReference(String pkg, String arg) {
        super(AppReferenceObj.TYPE, AppReferenceObj.json(pkg, arg));
    }

    public AppReference(String pkg, String arg, String state, String b64JpgThumbnail, String thumbText) {
        super(AppReferenceObj.TYPE, AppReferenceObj.json(pkg, arg, state, b64JpgThumbnail, thumbText));
    }

    public String getThumbnailImage() {
        if (mJson.has(AppReferenceObj.THUMB_JPG)) {
            return mJson.optString(AppReferenceObj.THUMB_JPG);
        }
        return null;
    }

    public String getThumbnailText() {
        if (mJson.has(AppReferenceObj.THUMB_TEXT)) {
            return mJson.optString(AppReferenceObj.THUMB_TEXT);
        }
        return null;
    }
}
