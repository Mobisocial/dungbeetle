package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.lang.ref.SoftReference;
import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.multiplayer.Multiplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.AppCorralActivity;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class AppObj extends DbEntryHandler implements Activator, FeedRenderer {
    private static final String TAG = "musubi-appObj";
    private static final boolean DBG = false;

    public static final String TYPE = "app";
    public static final String ANDROID_PACKAGE_NAME = "android_pkg";
    public static final String ANDROID_CLASS_NAME = "android_cls";
    public static final String ANDROID_ACTION = "android_action";
    public static final String WEB_URL = "web_url";

    @Override
    public String getType() {
        return TYPE;
    }

    public static DbObject fromPickerResult(Context context, String action,
            ResolveInfo resolveInfo, Intent pickerResult) {
        long[] contactIds = pickerResult.getLongArrayExtra("contacts");
        String pkgName = resolveInfo.activityInfo.packageName;
        String className = resolveInfo.activityInfo.name;

        /**
         * TODO:
         * 
         * Identity Firewall Goes Here.
         * Membership details can be randomized in one of many ways.
         * The app (scrabble) may see games a set of gamers play together.
         * The app may always see random ids
         * The app may always see stable ids
         * 
         * Can also permute the cursor and member order.
         */

        JSONArray participantIds = new JSONArray();
        
        participantIds.put(App.instance().getLocalPersonId());
        for (long id : contactIds) {
            Maybe<Contact> annoyingContact = Contact.forId(context, id);
            try {
                Contact contact = annoyingContact.get();
                participantIds.put(contact.personId);
            } catch (NoValError e) {
                participantIds.put(Contact.UNKNOWN);
            }
        }
        JSONObject json = new JSONObject();
        try {
            json.put(Multiplayer.OBJ_MEMBERSHIP, (participantIds));
            json.put(ANDROID_ACTION, action);
            json.put(ANDROID_PACKAGE_NAME, pkgName);
            json.put(ANDROID_CLASS_NAME, className);
        } catch (JSONException e) {
            Log.d(TAG, "What? Impossible!", e);
        }
        return new DbObject(TYPE, json);
    }

    public static DbObject forConnectedApp(Context context, ResolveInfo info) {
        /**
         * TODO:
         * 
         * Identity Firewall Goes Here.
         * Membership details can be randomized in one of many ways.
         * The app (scrabble) may see games a set of gamers play together.
         * The app may always see random ids
         * The app may always see stable ids
         * 
         * Can also permute the cursor and member order.
         */

        JSONObject json = new JSONObject();
        try {
            json.put(ANDROID_ACTION, Multiplayer.ACTION_CONNECTED);
            json.put(ANDROID_PACKAGE_NAME, info.activityInfo.packageName);
            json.put(ANDROID_CLASS_NAME, info.activityInfo.name);
        } catch (JSONException e) {
            Log.d(TAG, "What? Impossible!", e);
        }
        return new DbObject(TYPE, json);
    }

    @Override
    public void activate(Context context, SignedObj obj) {
        if (!(obj instanceof DbObj)) {
            Log.w(TAG, "Obj not ready yet!");
            return;
        }
        if (DBG) {
            JSONObject content = obj.getJson();
            Log.d(TAG, "activating app " + content + " from " + obj.getHash());
        }

        Intent launch = getLaunchIntent(context, (DbObj)obj);
        if (!(context instanceof Activity)) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(launch);
    }

    public static Intent getLaunchIntent(Context context, DbObj obj) {
        JSONObject content = obj.getJson(); 
        if (content.has(ANDROID_PACKAGE_NAME)) {
            Uri appFeed = obj.getContainingFeed().getUri();
            String action = content.optString(ANDROID_ACTION);
            String pkgName = content.optString(ANDROID_PACKAGE_NAME);
            String className = content.optString(ANDROID_CLASS_NAME);
    
            Intent launch = new Intent(action);
            launch.setClassName(pkgName, className);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            // TODO: feed for related objs, not parent feed
            launch.putExtra(AppState.EXTRA_FEED_URI, appFeed);
            launch.putExtra(AppState.EXTRA_OBJ_HASH, obj.getHash());
            // TODO: Remove
            launch.putExtra("obj", content.toString());

            List<ResolveInfo> resolved = context.getPackageManager().queryIntentActivities(launch, 0);
            if (resolved.size() > 0) {
                return launch;
            }

            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName));
            return market;
        } else if (content.has(WEB_URL)) {
            Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(content.optString(WEB_URL)));
            app.setClass(context, AppCorralActivity.class);
            app.putExtra(Musubi.EXTRA_FEED_URI, Feed.uriForName(obj.getFeedName()));
            return app;
        }
        return null;
    }

    @Override
    public void render(final Context context, final ViewGroup frame, Obj obj, boolean allowInteractions) {
        PackageManager pm = context.getPackageManager();
        Drawable icon = null;
        String appName;
        if (obj.getJson() != null && obj.getJson().has(ANDROID_PACKAGE_NAME)) {
            appName = obj.getJson().optString(ANDROID_PACKAGE_NAME);
        } else {
            appName = "Unknown";
        }
        if (!(obj instanceof DbObj)) {
            if (appName.contains(".")) {
                appName = appName.substring(appName.lastIndexOf(".") + 1);
            }
            String text = "Preparing application " + appName + "...";
            // TODO: Show Market icon or app icon.
            TextView valueTV = new TextView(context);
            valueTV.setText(text);
            valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
            frame.addView(valueTV);
            return;
        }

        DbObj dbParentObj = (DbObj) obj;
        boolean rendered = false;

        Intent launch = getLaunchIntent(context, dbParentObj);
        List<ResolveInfo> infos = pm.queryIntentActivities(launch, 0);
        if (infos.size() > 0) {
            ResolveInfo info = infos.get(0);
            if (info.activityInfo.labelRes != 0) {
                appName = info.activityInfo.loadLabel(pm).toString();
                icon = info.loadIcon(pm);
            } else {
                appName = info.activityInfo.name;
            }
        } else {
            appName = obj.getJson().optString(ANDROID_PACKAGE_NAME);
            if (appName.contains(".")) {
                appName = appName.substring(appName.lastIndexOf(".") + 1);
            }
        }
         // TODO: Safer reference to containing view
        if (icon != null) {
            View parentView = (View)frame.getParent().getParent();
            ImageView avatar = (ImageView)parentView.findViewById(R.id.icon);
            avatar.setImageDrawable(icon);

            TextView label = (TextView)parentView.findViewById(R.id.name_text);
            label.setText(appName);
        }

        // TODO: obj.getLatestChild().render();
        String selection = getRenderableClause();
        String[] selectionArgs = null;
        Cursor cursor = dbParentObj.getSubfeed().query(selection, selectionArgs);
        if (cursor.moveToFirst()) {
            DbObj dbObj = App.instance().getMusubi().objForCursor(cursor);
            DbObjects.getFeedRenderer(dbObj.getType())
                    .render(context, frame, dbObj, allowInteractions);
            rendered = true;
        }

        if (!rendered) {
            String text;
            if (icon != null) {
                ImageView iv = new ImageView(context);
                iv.setImageDrawable(icon);
                iv.setAdjustViewBounds(true);
                iv.setMaxWidth(60);
                iv.setMaxHeight(60);
                iv.setLayoutParams(CommonLayouts.WRAPPED);
                frame.addView(iv);
                text = appName;
            } else {
                text = "New application: " + appName + ".";
            }
            // TODO: Show Market icon or app icon.
            TextView valueTV = new TextView(context);
            valueTV.setText(text);
            valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
            frame.addView(valueTV);
        }
    }

    static SoftReference<String> mRenderableClause;
    private String getRenderableClause() {
        if (mRenderableClause != null) {
            String renderable = mRenderableClause.get();
            if (renderable != null) {
                return renderable;
            }
        }
        StringBuffer allowed = new StringBuffer();
        String[] types = DbObjects.getRenderableTypes();
        for (String type : types) {
            if (!AppObj.TYPE.equals(type)) {
                allowed.append(",'").append(type).append("'");
            }
        }
        String clause = DbObject.TYPE + " in (" + allowed.substring(1) + ")";
        mRenderableClause = new SoftReference<String>(clause);
        return clause;
    }
}