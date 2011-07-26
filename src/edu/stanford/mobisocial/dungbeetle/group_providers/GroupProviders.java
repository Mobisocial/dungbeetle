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
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
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
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.objects.JoinNotificationObj;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

public class GroupProviders{

    public static final String TAG = "GroupProviders";
    static final boolean DBG = false;

    private static List<GroupProvider> mHandlers = 
        new ArrayList<GroupProvider>();

    static {
        mHandlers.add(new PrplGroupProvider());
    }

    public static Uri defaultNewSessionUri(IdentityProvider ident, String groupName, String feedName){
        return (new PrplGroupProvider()).newSessionUri(ident, groupName, feedName);
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
        abstract public Uri newSessionUri(IdentityProvider ident, String groupName, String feedName);
        public void forceUpdate(final long groupId, final Uri uriIn, 
                                final Context context, final IdentityProvider ident, final int version){
            (new Thread(){
                    public void run(){
                        GroupProvider.this.handle(groupId, uriIn, context, ident, version, true);
                        
                        DBHelper helper = new DBHelper(context);
                        Maybe<Group> mg = helper.groupForGroupId(groupId);
                        try{
                            // group exists already, load view
                            Group g = mg.get();
                            Uri mFeedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + g.feedName);
                            //Helpers.sendToFeed(context, JoinNotificationObj.from(uriIn.toString()), mFeedUri);
                        }
                        catch(Maybe.NoValError e){
                            // group does not exist yet, time to prompt for join

                        }
                        
                        helper.close();
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
        public Uri newSessionUri(IdentityProvider ident, String groupName, String feedName){
            return Uri.parse("http://example.com/no_group");
        }
        public boolean willHandle(Uri uri){ return true; }
        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, final IdentityProvider ident, final int version, final boolean updateProfile){}
    }
    
    public static class PrplGroupProvider extends GroupProvider{
        
        public String groupName(Uri uri){
            return uri.getQueryParameter("groupName");
        }

        public String feedName(Uri uri){
            return uri.getQueryParameter("session");
        }

        public Uri newSessionUri(IdentityProvider ident, String groupName, String feedName){
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(DungBeetleActivity.GROUP_SESSION_SCHEME);
            builder.authority("suif.stanford.edu");
            builder.appendPath("dungbeetle");
            builder.appendPath("index.php");
            builder.appendQueryParameter("session", feedName);
            builder.appendQueryParameter("groupName", groupName);
            builder.appendQueryParameter("key", Base64.encodeToString(Util.newAESKey(), false));
            Uri uri = builder.build();
            return uri;
        }

        public boolean willHandle(Uri uri){
            return uri.getAuthority().equals("suif.stanford.edu");
        }

        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, final IdentityProvider ident, int version, boolean updateProfile){

            try{
                final byte[] key = Base64.decode(uriIn.getQueryParameter("key"));

                // Build uri we will send to server
                Uri.Builder b = new Uri.Builder();
                b.scheme("http");
                b.authority("suif.stanford.edu");
                b.path("dungbeetle/index.php");
                Uri uri = b.build();

                if (DBG) Log.i(TAG, "Doing dynamic group update for " + uri);

                
                StringBuffer sb = new StringBuffer();
                DefaultHttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(uri.toString());

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            
                final String pubKey = DBIdentityProvider.publicKeyToString(ident.userPublicKey());
                final String encryptedPubKey = Util.encryptAES(pubKey,key);
                final String feedName = uriIn.getQueryParameter("session");
                nameValuePairs.add(new BasicNameValuePair("public_key", encryptedPubKey));
                nameValuePairs.add(new BasicNameValuePair("email", Util.encryptAES(ident.userEmail(), key)));
                //if(updateProfile){
                  //  Log.w(TAG, ident.userProfile());
                    nameValuePairs.add(new BasicNameValuePair("profile", Util.encryptAES(ident.userProfile(), key)));
                //}
                nameValuePairs.add(new BasicNameValuePair("session", feedName));
                nameValuePairs.add(new BasicNameValuePair("version", Integer.toString(version)));
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

                //Log.i(TAG, response);

                if(response.equals("1"))
                {    
                    sb = new StringBuffer();
                    client = new DefaultHttpClient();
                    httpPost = new HttpPost(uri.toString());

                    nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("public_key", encryptedPubKey));
                    nameValuePairs.add(new BasicNameValuePair("email", Util.encryptAES(ident.userEmail(), key)));
                    
                    nameValuePairs.add(new BasicNameValuePair("profile", Util.encryptAES(ident.userProfile(), key)));
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

                    response = sb.toString();
                    JSONObject group = new JSONObject(response);
                    version = Integer.parseInt(group.getString("version"));
                    Helpers.updateGroupVersion(context, groupId, version);
                    JSONArray arr = new JSONArray(group.getString("users"));
                    for(int i = 0; i < arr.length(); i++) {
                        try {
                            String objStr = arr.getString(i);
                            JSONObject o = new JSONObject(objStr);
                            String encryptedPubK = o.getString("public_key");
                            final String pubKeyStr = Util.decryptAES(encryptedPubK, key);
                            final String email = Util.decryptAES(o.getString("email"), key);

                            if(email.equals(ident.userEmail())){
                                continue;
                            }

                            final String encryptedProfile = o.getString("profile");
                            //final String groupSession = o.getString("group_session");
                            final String idInGroup = o.getString("group_id");
                            (new Handler(context.getMainLooper())).post(new Runnable(){
                                    public void run(){

                                        ContentValues values = new ContentValues();
                                        values.put(Contact.PUBLIC_KEY, pubKeyStr);

                                        String profile = "";
                                        if(encryptedProfile != "null" && encryptedProfile != "" && encryptedProfile != null) {
                                            //Log.w(TAG, "["+encryptedProfile+"]");
                                            profile = Util.decryptAES(encryptedProfile, key);
                                        }
                                        if(!profile.equals("")) {
                                            try{
                                                JSONObject profileJSON = new JSONObject(profile);
                                                values.put(Contact.NAME, profileJSON.getString("name"));
                                                Log.w(TAG, profileJSON.getString("picture"));
                                                values.put(Contact.PICTURE, Base64.decode(profileJSON.getString("picture")));
                                            }
                                            catch(Exception e){
                                            }
                                        }
                                        else {
                                            values.put(Contact.NAME, email);
                                        }
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
            }
            catch(Exception e){
                Log.e(TAG, "Error in group provider.", e);
            }
        }
    }
}
