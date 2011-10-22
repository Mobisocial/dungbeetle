package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.multiplayer.Multiplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.AppState;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class AppObj extends DbEntryHandler implements Activator, FeedRenderer {
    private static final String TAG = "musubi-appObj";
    private static final boolean DBG = false;

    public static final String TYPE = "app";
    public static final String ANDROID_PACKAGE_NAME = "android_pkg";
    public static final String ANDROID_CLASS_NAME = "android_cls";
    public static final String ANDROID_ACTION = "android_action";

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
        if (DBG) {
            Uri feedUri = obj.getContainingFeed().getUri();
            JSONObject content = obj.getJson();
            Log.d(TAG, "activating app " + content + " for " + feedUri + ", " + obj.getHash());
        }

        Intent launch = getLaunchIntent(context, obj);
        if (!(context instanceof Activity)) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(launch);
    }

    public static Intent getLaunchIntent(Context context, SignedObj obj) {
        JSONObject content = obj.getJson(); 
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
    }

    @Override
    public void render(final Context context, final ViewGroup frame, Obj obj, boolean allowInteractions) {
        boolean rendered = false;

        if (!(obj instanceof DbObj)) {
            String appName = obj.getJson().optString(ANDROID_PACKAGE_NAME);
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
        String selection = "type = ?";
        String[] selectionArgs = new String[] { AppStateObj.TYPE };
        Cursor cursor = dbParentObj.getRelatedFeed().query(selection, selectionArgs);
        if (cursor.moveToFirst()) {
            DbObj dbObj = App.instance().getMusubi().objForCursor(cursor);
            AppState ref = new AppState(dbObj);
            String thumbnail = ref.getThumbnailImage();
            if (thumbnail != null) {
                rendered = true;
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                              LinearLayout.LayoutParams.WRAP_CONTENT,
                                              LinearLayout.LayoutParams.WRAP_CONTENT));
                App.instance().objectImages.lazyLoadImage(thumbnail.hashCode(), thumbnail, imageView);
                frame.addView(imageView);
            }
    
            thumbnail = ref.getThumbnailText();
            if (thumbnail != null) {
                rendered = true;
                TextView valueTV = new TextView(context);
                valueTV.setText(thumbnail);
                valueTV.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                valueTV.setGravity(Gravity.TOP | Gravity.LEFT);
                frame.addView(valueTV);
            }
    
            thumbnail = ref.getThumbnailHtml();
            if (thumbnail != null) {
                rendered = true;
                WebView webview = new WebView(context);
                webview.loadData(thumbnail, "text/html", "UTF-8");
                webview.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                Object o = frame.getTag(R.id.object_entry);
                webview.setOnTouchListener(new WebViewClickListener(webview, frame, (Integer)o));
                frame.addView(webview);
            }
        }

        if (!rendered) {
            String appName;
            Intent launch = getLaunchIntent(context, dbParentObj);
            List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(launch, 0);
            if (infos.size() > 0) {
                ResolveInfo info = infos.get(0);
                if (info.activityInfo.labelRes != 0) {
                    appName = info.activityInfo.loadLabel(context.getPackageManager()).toString();
                } else {
                    appName = info.activityInfo.name;
                }
            } else {
                appName = obj.getJson().optString(ANDROID_PACKAGE_NAME);
                if (appName.contains(".")) {
                    appName = appName.substring(appName.lastIndexOf(".") + 1);
                }
            }
            String text = "New application: " + appName + ".";
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

    private class WebViewClickListener implements View.OnTouchListener {
        private int position;
        private ViewGroup vg;
        private ViewGroup frame;
        private ListView lv;

        public WebViewClickListener(WebView wv, ViewGroup vg, int position) {
            this.vg = vg;
            this.position = position;
        }

        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_CANCEL:
                    return true;
                case MotionEvent.ACTION_UP:
                    sendClick();
                    return true;
            }

            return false;
        }

        public void sendClick() {
            if (lv == null) {
                while (!(vg instanceof ListView)) {
                    if (null != vg.getTag(R.id.object_entry)) {
                        frame = vg;
                    }
                    vg = (ViewGroup)vg.getParent();
                }
                lv = (ListView) vg;
            }
            lv.performItemClick(frame, position, 0);
        }
    }
}