package edu.stanford.mobisocial.dungbeetle;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.DbUser;
import mobisocial.socialkit.musubi.Musubi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class AppCorralActivity extends MusubiBaseActivity {
    private static final String EXTRA_CURRENT_PAGE = "page";
    private static final String MUSUBI_JS = "Musubi_android_platform";
    private String mCurrentPage;
    private SocialKitJavascript mSocialKitJavascript;
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcorral);
        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getString(EXTRA_CURRENT_PAGE);
        } else {
            mCurrentPage = "http://musubi.us/apps";
        }
        WebViewClient webViewClient = new AppStoreWebViewClient();
        
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        //mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setWebViewClient(webViewClient);
        mSocialKitJavascript = new SocialKitJavascript(this,
                (Uri)getIntent().getParcelableExtra(Musubi.EXTRA_FEED_URI));
        mWebView.addJavascriptInterface(mSocialKitJavascript, MUSUBI_JS);
        mWebView.loadUrl(mCurrentPage);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the BACK key and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the BACK key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
    
    class AppStoreWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getScheme().startsWith("http")) {
                if (!uri.getPath().endsWith(".apk")) {
                    return false;   
                }
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (DBG) Log.d(TAG, "Page loaded, injecting musubi SocialKit bridge for " + url);
            mCurrentPage = url;

            //mWebView.loadUrl("javascript:document.write(\"<script>" + js + "</script>\")");
            // Launch musubi app
            DbFeed dbFeed = mSocialKitJavascript.mDbFeed;
            SocialKitJavascript.Feed feed = mSocialKitJavascript.new Feed(dbFeed);
            SocialKitJavascript.User user = mSocialKitJavascript.new User(
                    App.instance().getMusubi().userForLocalDevice(dbFeed.getUri()));
            String initSocialKit = new StringBuilder("javascript:")
                .append("Musubi._launch(").append(
                        user.toJson() + ", " + feed.toJson() + ",'someappid', false)").toString();
            Log.d(TAG, "Android calling " + initSocialKit);
            mWebView.loadUrl(initSocialKit);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            if (DBG) {
                Log.d(TAG, "socialkit.js error: " + errorCode + ", " + description);
            }
        }
    }

    static class SocialKitJavascript {
        private static final String TAG = "socialkit.js";
        final Context mContext;
        final Musubi mMusubi;
        final DbFeed mDbFeed;

        SocialKitJavascript(Activity context, Uri feedUri) {
            mContext = context;
            mMusubi= Musubi.getInstance(context);
            mDbFeed = mMusubi.getFeed(feedUri);
        }

        public void _messagesForFeed(String feedName, String callback) {
            Log.d(TAG, "message for " + feedName);
        }
        
        public void _postObjToFeed(String obj, String feedName) {
            Log.d(TAG, "posting to " + feedName);
        }

        public void _setConfig(String config) {
            Log.d(TAG, "config " + config);
        }

        public void _log(String text) {
            Log.d(TAG, text);
        }

        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

        public Feed getFeed() {
            return new Feed(mDbFeed);
        }

        public boolean isDeveloperModeEnabled() {
            return MusubiBaseActivity.isDeveloperModeEnabled(mContext);
        }

        public void _runCommand(String className, String methodName, Object parameters, Object callback) {
            Log.d(TAG, "SOCIALKIT-ANDROID RAN " + className + "::" + methodName);
        }

        /**
         * JSON representations of common Musubi classes.
         */
        abstract class SocialKitApiConversion<JavaType> implements Jsonable {
            private final JavaType mNativeType;
            public SocialKitApiConversion(JavaType nativeType) {
                mNativeType = nativeType;
            }

            public JavaType getNative() {
                return mNativeType;
            }
        }

        class User extends SocialKitApiConversion<DbUser> {
            public User(DbUser user) {
                super(user);
            }

            @Override
            public JSONObject toJson() {
                DbUser user = getNative();
                JSONObject o = new JSONObject();
                try {
                    o.put("name", user.getName());
                    o.put("id", user.getLocalId());
                    o.put("personId", user.getId());
                } catch (JSONException e) {}
                return o;
            }
        }

        class Obj extends SocialKitApiConversion<DbObj> {
            public Obj(DbObj obj) {
                super(obj);
            }

            @Override
            public JSONObject toJson() {
                DbObj obj = getNative();
                JSONObject json = obj.getJson();
                try {
                    json.put("type", obj.getType());
                    json.put("data", json);
                } catch (JSONException e) {}
                return json;
            }
        }

        class Feed extends SocialKitApiConversion<DbFeed> {
            public Feed(DbFeed feed) {
                super(feed);
            }

            @Override
            public JSONObject toJson() {
                DbFeed feed = getNative();
                JSONObject o = new JSONObject();
                try {
                    o.put("name", feed.getUri().getLastPathSegment());
                    o.put("uri", feed.getUri().toString());
                    o.put("session", feed.getUri().getLastPathSegment());
                    o.put("key", "what is a key?");
                    JSONArray m = new JSONArray();
                    for (DbUser u : feed.getRemoteUsers()) {
                        m.put(new User(u).toJson());
                    }
                    o.put("members", m);
                } catch (JSONException e) {}
                return o;
            }
        }

        interface Jsonable {
            public JSONObject toJson();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CURRENT_PAGE, mCurrentPage);
    }
}
