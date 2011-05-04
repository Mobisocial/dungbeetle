package edu.stanford.mobisocial.dungbeetle.google;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.stanford.mobisocial.dungbeetle.R;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Entry point in the application.
 * Launches the OAuth flow by starting the PrepareRequestTokenActivity
 *
 */
public class OAuthFlowApp extends Activity {

	private static final int PICK_CONTACT = 0;
	final String TAG = getClass().getName();
	private SharedPreferences prefs;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_login);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Button launchOauth = (Button) findViewById(R.id.btn_launch_oauth);
        launchOauth.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent().setClass(v.getContext(), PrepareRequestTokenActivity.class));
                }
            });

        Button clearCredentials = (Button) findViewById(R.id.btn_clear_credentials);
        clearCredentials.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    clearCredentials();
                    refreshProperties();
                }
            });


        Button refresh = (Button) findViewById(R.id.btn_refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    refreshProperties();
                }
            });

        Button deleteProp = (Button) findViewById(R.id.btn_delete);
        deleteProp.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try{
                        doDelete(Constants.RESOURCE_URL + "/0",   getConsumer(OAuthFlowApp.this.prefs));
                    } catch (Exception e) {
                        Log.e(TAG, "Error executing request",e);
                    }
                    refreshProperties();
                }
            });

        Button setProp = (Button)findViewById(R.id.btn_set_prop);
        setProp.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(OAuthFlowApp.this);
                    alert.setMessage("Enter value of 'foo' property:");
                    final EditText input = new EditText(OAuthFlowApp.this);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String value = input.getText().toString();
                                Map<String,String> params = new HashMap<String,String>();
                                try{
                                    JSONObject obj = new JSONObject();
                                    obj.put("name", "foo");
                                    obj.put("value", value);
                                    try {

                                        doPost(Constants.RESOURCE_URL, 
                                               obj.toString(), 
                                               "application/json", 
                                               getConsumer(OAuthFlowApp.this.prefs));
                                        refreshProperties();

                                    } catch (Exception e) {
                                        Log.e(TAG, "Error executing request",e);
                                    }
                                }
                                catch(JSONException e){
                                    Log.e(TAG, "Error building payload",e);
                                }
                            }
                        });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {}
                        });
                    alert.show();

                    
                }
            });

        refreshProperties();
    }

	private void refreshProperties() {
		TextView textView = (TextView) findViewById(R.id.response_code);
		String result = "";
        try {
        	result = doGet(Constants.RESOURCE_URL, getConsumer(this.prefs));
        	textView.setText(result);
		} catch (Exception e) {
			Log.e(TAG, "Error executing request",e);
			textView.setText("Error retrieving properties: " + result);
		}
	}
	
    private void clearCredentials() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final Editor edit = prefs.edit();
		edit.remove(OAuth.OAUTH_TOKEN);
		edit.remove(OAuth.OAUTH_TOKEN_SECRET);
		edit.commit();
	}

	
	private OAuthConsumer getConsumer(SharedPreferences prefs) {
		String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
		String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);
		consumer.setTokenWithSecret(token, secret);
		return consumer;
	}

	
	private String doGet(String url, OAuthConsumer consumer) throws Exception {
		DefaultHttpClient httpclient = new DefaultHttpClient();
    	HttpGet request = new HttpGet(url);
    	Log.i(TAG,"Requesting URL : " + url);
    	consumer.sign(request);
    	HttpResponse response = httpclient.execute(request);
    	Log.i(TAG,"Statusline : " + response.getStatusLine());
    	InputStream data = response.getEntity().getContent();
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));
        String responeLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responeLine = bufferedReader.readLine()) != null) {
        	responseBuilder.append(responeLine);
        }
        Log.i(TAG,"Response : " + responseBuilder.toString());
        return responseBuilder.toString();
	}


	private String doDelete(String url, OAuthConsumer consumer) throws Exception {
		DefaultHttpClient httpclient = new DefaultHttpClient();
    	HttpDelete request = new HttpDelete(url);
    	Log.i(TAG,"Requesting URL : " + url);
    	consumer.sign(request);
    	HttpResponse response = httpclient.execute(request);
    	Log.i(TAG,"Statusline : " + response.getStatusLine());
    	InputStream data = response.getEntity().getContent();
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));
        String responeLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responeLine = bufferedReader.readLine()) != null) {
        	responseBuilder.append(responeLine);
        }
        Log.i(TAG,"Response : " + responseBuilder.toString());
        return responseBuilder.toString();
	}


	private String doPost(String url, 
                          String payload, 
                          String contentType,  
                          OAuthConsumer consumer) throws Exception {

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(url);
        StringEntity se = new StringEntity(payload,HTTP.UTF_8);
        se.setContentType(contentType);
        request.setEntity(se);
        consumer.sign(request);
        StringBuffer sb = new StringBuffer();
        try {
            HttpResponse execute = client.execute(request);
            InputStream content = execute.getEntity().getContent();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String s = "";
            while ((s = buffer.readLine()) != null) {
                sb.append(s);
            }
            return sb.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



}