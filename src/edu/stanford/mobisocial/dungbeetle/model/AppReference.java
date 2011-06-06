package edu.stanford.mobisocial.dungbeetle.model;

import org.json.JSONObject;

import android.content.Intent;

import edu.stanford.mobisocial.dungbeetle.objects.AppReferenceObj;

public class AppReference extends DbObject {
    public static final String APPLICATION_ARGUMENT = "android.intent.extra.APPLICATION_ARGUMENT";

    public AppReference(JSONObject json) {
        super(AppReferenceObj.TYPE, json);
    }

    public static AppReference fromIntent(Intent intent) {
        String arg = intent.getStringExtra(APPLICATION_ARGUMENT);
        String pkg = intent.getStringExtra("mobisocial.db.PACKAGE");
        String state = intent.getStringExtra("mobisocial.db.STATE");
        return new AppReference(arg, pkg, state);
    }

    public String pkg() {
        return this.mJson.optString("packageName");
    }

    public AppReference(String pkg, String arg) {
        super(AppReferenceObj.TYPE, AppReferenceObj.json(pkg, arg));
    }

    public AppReference(String pkg, String arg, String state) {
        super(AppReferenceObj.TYPE, AppReferenceObj.json(pkg, arg, state));
    }

}
