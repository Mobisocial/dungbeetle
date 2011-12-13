package edu.stanford.mobisocial.dungbeetle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class AppCorralActivity extends MusubiBaseActivity {
    private static final String EXTRA_CURRENT_PAGE = "page";
    private static final String MUSUBI_JS = "Musubi_android_platform";
    private String mCurrentPage;
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
        mWebView.addJavascriptInterface(new SocialKitJavascript(this,
                (Uri)getIntent().getParcelableExtra(Musubi.EXTRA_FEED_URI)), MUSUBI_JS);
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
            SocialKitJavascript.Obj obj = new SocialKitJavascript.Obj();
            SocialKitJavascript.Feed feed = new SocialKitJavascript.Feed("feedName");
            SocialKitJavascript.User user = new SocialKitJavascript.User("todoName", "todoId", "todoPersonId");
            String initSocialKit = new StringBuilder("javascript:")
                .append("Musubi._launch(\"string\", ")
                .append(user.toJson() + ", " + feed.toJson() + ",'someappid', {})").toString();
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
            return new Feed(mDbFeed.getUri().getLastPathSegment());
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

        static class User implements Jsonable {
            String name;
            String id;
            String personId;

            public User(String name, String id, String personId) {
                this.name = name;
                this.id = id;
                this.personId = personId;
            }

            @Override
            public JSONObject toJson() {
                JSONObject o = new JSONObject();
                try {
                    o.put("name", name);
                    o.put("id", id);
                    o.put("personId", personId);
                } catch (JSONException e) {}
                return o;
            }
            
        }

        static class Obj implements Jsonable {
            public String type;
            public JSONObject data;

            public Obj() {
                type = "dumbtype";
                data = new JSONObject();

                try {
                    JSONArray members = new JSONArray();
                    members.put(new User("alfred", "123", "456").toJson());
                    members.put(new User("brian", "789", "0ab").toJson());
                    data.put("membership", members);
                } catch (JSONException e) {}
            }

            @Override
            public JSONObject toJson() {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", type);
                    json.put("data", data);
                } catch (JSONException e) {}
                return json;
            }
        }

        static class Feed implements Jsonable {
            public String name;
            public String uri;
            public String session;
            public String key;

            public Feed(String name) {
                this.name = name;
                this.uri = "feeduri";
                this.session = "feedsession";
                this.key = "feedkey";
            }

            public String getName() {
                return name;
            }

            @Override
            public JSONObject toJson() {
                JSONObject o = new JSONObject();
                try {
                    o.put("name", name);
                    o.put("uri", uri);
                    o.put("session", session);
                    o.put("key", key);
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
