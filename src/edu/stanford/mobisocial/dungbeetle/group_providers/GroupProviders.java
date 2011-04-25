package edu.stanford.mobisocial.dungbeetle.group_providers;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.bumblebee.util.Base64;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleActivity;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.GroupManagerThread.GroupRefreshHandler;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.util.Util;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupProviders{

    public static final String TAG = "GroupProviders";

    private static List<GroupProvider> mHandlers = 
        new ArrayList<GroupProvider>();

    static {
        mHandlers.add(new PrplGroupProvider());
    }

    public static Uri defaultNewSessionUri(IdentityProvider ident, String groupName){
        return (new PrplGroupProvider()).newSessionUri(ident, groupName);
    }

    public static GroupProvider forUri(Uri uri){
        for(final GroupProvider h : mHandlers){
            if(h.willHandle(uri)){
                return h;
            }
        }
        return new NullGroupProvider();
    }

    public static abstract class GroupProvider implements GroupRefreshHandler{
        abstract public String groupName(Uri uri);
        abstract public String feedName(Uri uri);
        abstract public Uri newSessionUri(IdentityProvider ident, String groupName);
        public void forceUpdate(final long groupId, final Uri uriIn, 
                                final Context context, final IdentityProvider ident){
            (new Thread(){
                    public void run(){
                        GroupProvider.this.handle(groupId, uriIn, context, ident);
                    }
                }).start();
        }
    }

    public static class NullGroupProvider extends GroupProvider{
        public String groupName(Uri uri){
            return "NA";
        }
        public String feedName(Uri uri){
            return "NA";
        }
        public Uri newSessionUri(IdentityProvider ident, String groupName){
            return Uri.parse("http://example.com/no_group");
        }
        public boolean willHandle(Uri uri){ return true; }
        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, final IdentityProvider ident){}
    }
    
    public static class PrplGroupProvider extends GroupProvider{
        
        public String groupName(Uri uri){
            return uri.getQueryParameter("groupName");
        }

        public String feedName(Uri uri){
            return uri.getQueryParameter("session");
        }

        public Uri newSessionUri(IdentityProvider ident, String groupName){
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(DungBeetleActivity.GROUP_SESSION_SCHEME);
            builder.authority("suif.stanford.edu");
            builder.appendPath("dungbeetle");
            builder.appendPath("index.php");
            builder.appendQueryParameter("session", Util.MD5("session" + Math.random()));
            builder.appendQueryParameter("groupName", groupName);
            builder.appendQueryParameter("key", Base64.encodeToString(Util.newAESKey(), false));
            Uri uri = builder.build();
            return uri;
        }

        public boolean willHandle(Uri uri){
            return uri.getAuthority().equals("suif.stanford.edu");
        }

        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, final IdentityProvider ident){

            try{
                final byte[] key = Base64.decode(uriIn.getQueryParameter("key"));

                // Build uri we will send to server
                Uri.Builder b = new Uri.Builder();
                b.scheme("http");
                b.authority("suif.stanford.edu");
                b.path("dungbeetle/index.php");
                Uri uri = b.build();

                Log.i(TAG, "Doing dynamic group update for " + uri);
                StringBuffer sb = new StringBuffer();
                DefaultHttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(uri.toString());

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                final String pubKey = DBIdentityProvider.publicKeyToString(ident.userPublicKey());
                final String encryptedPubKey = Util.encryptAES(pubKey,key);
                final String feedName = uriIn.getQueryParameter("session");
                nameValuePairs.add(new BasicNameValuePair("public_key", encryptedPubKey));
                nameValuePairs.add(new BasicNameValuePair("email", Util.encryptAES(ident.userEmail(), key)));
                nameValuePairs.add(new BasicNameValuePair("profile", Util.encryptAES("", key)));
                nameValuePairs.add(new BasicNameValuePair("session", feedName));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                try {
                    HttpResponse execute = client.execute(httpPost);
                    InputStream content = execute.getEntity().getContent();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        sb.append(s);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                String response = sb.toString();
                JSONArray arr = new JSONArray(response);
                for(int i = 0; i < arr.length(); i++) {
                    try {
                        String objStr = arr.getString(i);
                        JSONObject o = new JSONObject(objStr);
                        String encryptedPubK = o.getString("public_key");
                        final String pubKeyStr = Util.decryptAES(encryptedPubK, key);
                        final String email = Util.decryptAES(o.getString("email"), key);
                        final String profile = Util.decryptAES(o.getString("profile"), key);
                        final String groupSession = o.getString("group_session");
                        final String idInGroup = o.getString("group_id");
                        (new Handler(context.getMainLooper())).post(new Runnable(){
                                public void run(){
                                    ContentValues values = new ContentValues();
                                    values.put(Contact.PUBLIC_KEY, pubKeyStr);
                                    values.put(Contact.NAME, email);
                                    values.put(Contact.EMAIL, email);
                                    values.put(Group.FEED_NAME, feedName);
                                    values.put(GroupMember.GLOBAL_CONTACT_ID, idInGroup);
                                    values.put(GroupMember.GROUP_ID, groupId);
                                    Uri url = Uri.parse(
                                        DungBeetleContentProvider.CONTENT_URI + 
                                        "/dynamic_group_member");
                                    context.getContentResolver().insert(url, values);
                                }
                            });
                    }
                    catch(Exception e){
                        Log.e(TAG, "Error processing dynamic group contact.", e);
                    }
                }
            }
            catch(Exception e){
                Log.e(TAG, "Error in group provider.", e);
            }
        }
    }
}
