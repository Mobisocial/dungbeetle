package edu.stanford.mobisocial.dungbeetle.group_providers;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.HandleGroupSessionActivity;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import edu.stanford.mobisocial.dungbeetle.GroupManagerThread.GroupRefreshHandler;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupProviders{

    public static final String TAG = "GroupProviders";

    public static String groupName(Uri uri){
        return uri.getQueryParameter("groupName");
    }

    public static Uri newSessionUri(IdentityProvider ident, String groupName){
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(HandleGroupSessionActivity.SCHEME);
        builder.authority("suif.stanford.edu");
        builder.appendPath("dungbeetle");
        builder.appendPath("index.php");
        builder.appendQueryParameter("session", Util.MD5("session" + Math.random()));
        builder.appendQueryParameter("groupName", groupName);
        Uri uri = builder.build();
        return uri;
    }

    public static class PrplGroupRefreshHandler extends GroupRefreshHandler{
        public boolean willHandle(Uri uri){
            return uri.getAuthority().equals("suif.stanford.edu");
        }
        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, final IdentityProvider ident){
            Uri.Builder b = uriIn.buildUpon();
            b.appendQueryParameter(
                "public_key", 
                DBIdentityProvider.publicKeyToString(ident.userPublicKey()));
            b.appendQueryParameter("email", ident.userEmail());
            b.scheme("http");
            Uri uri = b.build();
            Log.i(TAG, "Doing dynamic group update for " + uri);
            StringBuffer sb = new StringBuffer();
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(uri.toString());
            try {
                HttpResponse execute = client.execute(httpGet);
                InputStream content = execute.getEntity().getContent();
                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String s = "";
                while ((s = buffer.readLine()) != null) {
                    sb.append(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String response = sb.toString();
            Log.i(TAG, "Got response: " + response);
            try{
                JSONArray arr = new JSONArray(response);
                for(int i = 0; i < arr.length(); i++){
                    String objStr = arr.getString(i);
                    JSONObject o = new JSONObject(objStr);
                    final String pubKeyStr = o.getString("public_key");
                    final String email = o.getString("email");
                    final String profile = o.getString("profile");
                    final String groupSession = o.getString("group_session");
                    final String idInGroup = o.getString("group_id");
                    (new Handler(context.getMainLooper())).post(new Runnable(){
                            public void run(){
                                Uri u = Helpers.insertContact(context, pubKeyStr, email, email);
                                Long cid = Long.valueOf(u.getLastPathSegment());
                                Helpers.insertGroupMember(context, groupId, cid, idInGroup);
                            }
                        });
                }
            } catch(JSONException e){Log.e(TAG, e.getStackTrace().toString());}
        }
    }

}