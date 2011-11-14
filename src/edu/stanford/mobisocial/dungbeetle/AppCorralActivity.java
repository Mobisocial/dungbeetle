package edu.stanford.mobisocial.dungbeetle;

import mobisocial.socialkit.musubi.Musubi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class AppCorralActivity extends MusubiBaseActivity {
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcorral);

        WebViewClient webViewClient = new AppStoreWebViewClient();
        
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(webViewClient);
        mWebView.addJavascriptInterface(new SocialKitInterface(this,
                (Uri)getIntent().getParcelableExtra(Musubi.EXTRA_FEED_URI)), "SocialKit");
        mWebView.loadUrl("http://musubi.us/apps");
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
    }

    class SocialKitInterface {
        final Context mContext;
        final Uri mFeedUri;

        SocialKitInterface(Context c, Uri feedUri) {
            mContext = c;
            mFeedUri = feedUri;
        }

        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

        public String getFeed() {
            return mFeedUri.getLastPathSegment();
        }
    }
}
