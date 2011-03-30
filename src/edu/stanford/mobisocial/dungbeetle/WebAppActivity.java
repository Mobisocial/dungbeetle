package edu.stanford.mobisocial.dungbeetle;
import android.content.res.Configuration;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.webkit.WebView;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;

public class WebAppActivity extends Activity{
    WebView mWebView;
    String mArg;

    private class WebAppViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onReceivedError(WebView view, int errorCode, 
                                    String description, String failingUrl) {
            Toast.makeText(WebAppActivity.this, 
                           "Error: " + description, 
                           Toast.LENGTH_SHORT).show();
        }
    }

    private class WebAppChromeClient extends WebChromeClient {
        public void onProgressChanged(WebView view, int progress) {
            WebAppActivity.this.setProgress(progress * 1000);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_app);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebAppViewClient());
        mWebView.setWebChromeClient(new WebAppChromeClient());
        mWebView.addJavascriptInterface(new WebAppAPI(), "externalInterface"); 
		Intent intent = getIntent();
        Uri uri = intent.getData();
        mArg = intent.getStringExtra("android.intent.extra.APPLICATION_ARGUMENT");
		if(uri != null){
            mWebView.loadUrl(uri.toString());
		}
		else{
			Toast.makeText(this, "No URL given!", Toast.LENGTH_SHORT).show();
		}
    }

    // API to be exposed to web apps. 
    // BE CAREFUL HERE. Assume all javascript it malicious!
    // Also, remember these will be called from another thread.
    private class WebAppAPI{
        public void toast(final String s){
            WebAppActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        Toast.makeText(WebAppActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
        }
        public String getArgument(){
            return mArg;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle orientation change here
        // Maybe signal to javascript?
    }

}