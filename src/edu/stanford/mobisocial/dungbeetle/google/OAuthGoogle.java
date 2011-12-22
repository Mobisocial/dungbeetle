package edu.stanford.mobisocial.dungbeetle.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.ArrayList;

import mobisocial.socialkit.musubi.RSACrypto;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.social.FriendRequest;

public class OAuthGoogle extends Activity {
	final String TAG = getClass().getName();
	private static final String PREF_NAME = "oauth";
	private static final String[] PREF_TOKENS = {
		"google_access_token",
		"facebook_access_token"
	};
	private static final String[] ACCOUNT_TYPES = {
		"com.google",
		"com.facebook.auth.login"
	};
	private static final String[] AUTH_SCOPES = {
		"oauth2:http://www.google.com/m8/feeds/",
		"com.facebook.auth.login"
	};
	private static final int AUTH_GOOGLE = 0;
	private static final int AUTH_FACEBOOK = 1;



	private static final int REQUEST_AUTHENTICATE = 0;
	private static final int DIALOG_CHOOSE_ACCOUNT = 0;
	private static final int DIALOG_CHOOSE_FRIEND_TO_ADD = 1;


	Account[] accounts;
	SharedPreferences settings;
	AccountManager accountManager;
	int authenticator;
	ArrayList<Pair<String, String>> friendList;

	private final class OAuthGoogleListener implements OnClickListener {
		@Override
		public void onClick(View v) {	
			authenticator = AUTH_GOOGLE;
			accounts = accountManager.getAccountsByType(ACCOUNT_TYPES[authenticator]);
//			settings = this.getSharedPreferences("prefs", 0);
			if(accounts.length > 1) {
				showDialog(DIALOG_CHOOSE_ACCOUNT);
			} else {
				getAuthToken(accounts[0]);
			}
		}
	}

	private final class OAuthFacebookListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			authenticator = AUTH_FACEBOOK;
			accounts = accountManager.getAccountsByType(ACCOUNT_TYPES[authenticator]);
//			settings = this.getSharedPreferences("prefs", 0);
			if(accounts.length > 1) {
				showDialog(DIALOG_CHOOSE_ACCOUNT);
			} else {
				getAuthToken(accounts[0]);
			}
		}
	}

	private final class FindGoogleContactsListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			final SharedPreferences settings = v.getContext().getSharedPreferences(PREF_NAME, 0);
			String accessToken = settings.getString(PREF_TOKENS[authenticator], "");
			TextView textView = (TextView) findViewById(R.id.google_contacts);

			try {
				Uri.Builder builder = new Uri.Builder().scheme("http").authority("musulogin.appspot.com")
						.path("oauth2users").appendQueryParameter("token", accessToken);

				Log.i(TAG, builder.toString());

				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(builder.toString());
				HttpResponse response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				String line = "";
				String jsonStr = "";
				while ((line = rd.readLine()) != null) {
					//textView.append(line);
					jsonStr += line;
				}

				JSONObject object = new JSONObject(jsonStr);
				String keysStr = object.getString("keys");
				JSONArray keys = new JSONArray(keysStr);
				textView.setText("");

				friendList = new ArrayList<Pair<String, String>>(keys.length());
				for(int i = 0; i < keys.length(); i++){
					JSONObject friend = keys.getJSONObject(i);
					textView.append(friend.getString("key") + "->" + friend.getString("uid") + "\n");
					friendList.add(new Pair<String, String>(friend.getString("uid"), friend.getString("key")));
				}

				showDialog(DIALOG_CHOOSE_FRIEND_TO_ADD);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			} 
		}

	}

	@Override
	protected Dialog onCreateDialog(int id){
		Dialog dialog = null;
		switch(id) {
		case DIALOG_CHOOSE_ACCOUNT:
			final CharSequence[] items = new CharSequence[accounts.length];
			for(int i = 0; i < accounts.length; i++)
				items[i] = accounts[i].name;
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setTitle("Choose an account");
			builder1.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
			        getAuthToken(accounts[item]);
			        dialog.dismiss();
			    }
			});
			AlertDialog chooseAccountDlg = builder1.create();
			dialog = chooseAccountDlg;
			break;
		case DIALOG_CHOOSE_FRIEND_TO_ADD:
			final CharSequence[] friendIds = new CharSequence[friendList.size()];
			int i = 0;
			for(Pair<String, String> f : friendList){
				friendIds[i++] = f.first;
			}
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("Choose a friend");
			builder2.setSingleChoiceItems(friendIds, 0, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(getApplicationContext(), friendIds[item], Toast.LENGTH_SHORT).show();
			        //getAuthToken(accounts[item]);
			        addContactAsFriend(friendList.get(item).first, friendList.get(item).second);
			        dialog.dismiss();
			    }
			});
			AlertDialog chooseFriendDlg = builder2.create();
			dialog = chooseFriendDlg;
			break;
		default:
			dialog = null;
		}

		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.oauth_google);

		findViewById(R.id.authenticate_google).setOnClickListener(
				new OAuthGoogleListener());
		findViewById(R.id.authenticate_facebook).setOnClickListener(
				new OAuthFacebookListener());

		findViewById(R.id.find_google_contacts).setOnClickListener(
				new FindGoogleContactsListener());

		accountManager = AccountManager.get(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				getAuthToken(accounts[0]);
			} else {
				// user denied
			}
			break; 
		}
	}

	private void addContactAsFriend(String email, String pubkey){
		RSACrypto.publicKeyFromString(pubkey);
		Uri uri = Helpers.insertContact(this, pubkey, email, email);
		long contactId = Long.valueOf(uri.getLastPathSegment());
		Helpers.insertSubscriber(this, contactId, "friend");

		//FriendRequest.sendFriendRequest(this,contactId, null);
		String emailList[] = {email};
		Intent share = new Intent(Intent.ACTION_SEND);
		Uri friendRequest = FriendRequest.getInvitationUri(this);
		share.putExtra(Intent.EXTRA_TEXT,
				"Be my friend on Musubi! Click here from your Android device: "
						+ friendRequest);
		share.putExtra(Intent.EXTRA_SUBJECT, "Join me on Musubi!");
		share.putExtra(Intent.EXTRA_EMAIL, emailList);
		share.setType("text/plain");
		startActivity(share);
	}

	private void getAuthToken(final Account account) {

		final SharedPreferences settings = this.getSharedPreferences(PREF_NAME, 0);
		String accessToken = settings.getString(PREF_TOKENS[authenticator], "");

		accountManager.invalidateAuthToken(ACCOUNT_TYPES[authenticator], accessToken);
		accountManager.getAuthToken(account, AUTH_SCOPES[authenticator], true, new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					if (bundle.containsKey(AccountManager.KEY_INTENT)) {
						Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
						intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivityForResult(intent, REQUEST_AUTHENTICATE);
					} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
						//accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, bundle.getString(AccountManager.KEY_AUTHTOKEN));
						//new LoadActivities().execute();
						String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
						String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
						String accountType = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
						String msg = "{auth token -> " + authToken + "}"
								+ "{account type -> " + accountType + "}"
								+ "{account name -> " + accountName + "}";
						displayMsg(msg);

						if (accountName != null && authToken != null) {
				            final SharedPreferences.Editor editor = settings.edit();
				            editor.putString(PREF_TOKENS[authenticator], authToken);
				            editor.commit();
						}

						send(authToken);

						//getContacts(authToken);
						Log.i(TAG, msg);
					}
				} catch (Exception e) {
					Log.e(TAG, "Error during OAUth retrieve request token", e);
				} }
		}, null);
	}

	private void send(String token){
		DBHelper helper = DBHelper.getGlobal(this);
        IdentityProvider ident = new DBIdentityProvider(helper);
        try {
	        PublicKey pubKey = ident.userPublicKey();
	        helper.close();

	        Uri.Builder builder = new Uri.Builder().scheme("https").authority("musulogin.appspot.com")
	                .path("oauth2users").appendQueryParameter("token", token)
	                .appendQueryParameter("key", DBIdentityProvider.publicKeyToString(pubKey));

	        Log.i(TAG, builder.toString());

	        HttpClient client = new DefaultHttpClient();
	        HttpPost request = new HttpPost(builder.toString());
	        client.execute(request);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        } finally {
        	ident.close();
        }
	}

	private void displayMsg(String msg) {
		TextView textView = (TextView) findViewById(R.id.oauth_token);
		textView.setText(msg);
	}

	private void getContacts(String token) {
		String url = "https://www.google.com/m8/feeds/contacts/default/full";
		url += "?access_token=" + token;
		TextView textView = (TextView) findViewById(R.id.google_contacts);
		try {
			textView.setText("");
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				textView.append(line);
			}
		} catch (HttpResponseException e) {
			 if (e.getStatusCode() == 401) {
				 textView.setText(e.getMessage());
			 }
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
