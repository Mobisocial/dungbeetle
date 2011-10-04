package edu.stanford.mobisocial.dungbeetle.group_providers;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.bumblebee.util.Base64;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.JoinNotificationObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.GroupMember;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class GroupProviders {

    public static final String TAG = "GroupProviders";
    static final boolean DBG = MusubiBaseActivity.DBG;
    
    //we need to limit the group tasks to a single thread because otherwise
    //we might launch too many and consume too much memory/CPU
    static class TaskEntry {
    	public TaskEntry(long k, Runnable r) {
    		key = k;
    		runnable = r;
		}
    	long key;
    	Runnable runnable;
    }
    private static LinkedBlockingDeque<TaskEntry> g_group_tasks = new LinkedBlockingDeque<TaskEntry>();
    private static Thread g_group_thread = null;
    public static void runBackgroundGroupTask(long key, Runnable task) {
    	synchronized(g_group_tasks) {
    		for (Iterator<TaskEntry> i = g_group_tasks.iterator(); i.hasNext();) {
				TaskEntry t = i.next();
				if(t.key == key) {
					i.remove();
				}
			}
    		g_group_tasks.add(new TaskEntry(key, task));
    		if(g_group_thread == null) {
    			g_group_thread = new Thread("Group Worker Thread") {
	    			@Override
	    			public void run() {
	    				for(;;) {
	    					TaskEntry entry;
	    					synchronized (g_group_tasks) {
	    						if(g_group_tasks.size() == 0) {
	    							//we're done so we need a new thread for the next task
	    							g_group_thread = null;
	    							break;
	    						}
	    						 entry = g_group_tasks.remove();
	    					}
	    					try {
	    						entry.runnable.run();
	    					} catch(Throwable t) {
	    						Log.wtf(TAG, "uncaught exception in group task", t);
	    					}
	    				}
	    			}
	    	    };
    			g_group_thread.start();
    		}
    	}
    }

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
                                final Context context, final int version, final boolean broadcastPresence){
        	GroupProviders.runBackgroundGroupTask(groupId, new Runnable(){
                public void run(){
                	
                    GroupProvider.this.handle(groupId, uriIn, context, version, true);
                    
                    DBHelper helper = DBHelper.getGlobal(context);
                    Maybe<Group> mg = helper.groupForGroupId(groupId);
                    try{
                        // group exists already, load view
                        Group g = mg.get();
                        Uri feedUri = Feed.uriForName(g.feedName);
                        if (broadcastPresence) {
                        	Helpers.sendToFeed(context, JoinNotificationObj.from(uriIn.toString()), feedUri);
                        }
                    }
                    catch(Maybe.NoValError e){
                        // group does not exist yet, time to prompt for join

                    }
                    
                    helper.close();
                }
            });
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
                           final Context context, final int version, final boolean updateProfile){}
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
            builder.scheme(HomeActivity.GROUP_SESSION_SCHEME);
            builder.authority("suif.stanford.edu");
            builder.appendPath("dungbeetle");
            builder.appendPath("index.php");
            builder.appendQueryParameter("session", feedName);
            builder.appendQueryParameter("groupName", groupName);
            builder.appendQueryParameter("key", FastBase64.encodeToString(Util.newAESKey()));
            Uri uri = builder.build();
            return uri;
        }

        public boolean willHandle(Uri uri){
            return uri.getAuthority().equals("suif.stanford.edu");
        }

        public void handle(final long groupId, final Uri uriIn, 
                           final Context context, int version, boolean updateProfile){

            try{
                final byte[] key = FastBase64.decode(uriIn.getQueryParameter("key"));

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
            
                DBHelper mHelper = DBHelper.getGlobal(context);
                DBIdentityProvider ident = new DBIdentityProvider(mHelper);
                try {
	                final String pubKey = DBIdentityProvider.publicKeyToString(ident.userPublicKey());
	                final String encryptedPubKey = Util.encryptAES(pubKey,key);
	                final String feedName = uriIn.getQueryParameter("session");
	                
	   
	                client = new DefaultHttpClient();
	                httpPost = new HttpPost(uri.toString());
	
	                nameValuePairs = new ArrayList<NameValuePair>(2);
	                nameValuePairs.add(new BasicNameValuePair("public_key", encryptedPubKey));
	                nameValuePairs.add(new BasicNameValuePair("email", Util.encryptAES(ident.userEmail(), key)));
	                
	                //nameValuePairs.add(new BasicNameValuePair("profile", Util.encryptAES(ident.userProfile(), key)));
	                nameValuePairs.add(new BasicNameValuePair("version", Integer.toString(version)));
	                nameValuePairs.add(new BasicNameValuePair("session", feedName));
	                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	                try {
	                    HttpResponse execute = client.execute(httpPost);
	                    InputStream content = execute.getEntity().getContent();
	                    sb = new StringBuffer(IOUtils.toString(content));
	                    Log.e("WHOHO", "version: " + version + ", " + sb.length() + " group size");
	                }
	                catch (Exception e) {
		                sb = new StringBuffer();
	                    e.printStackTrace();
	                }
	
	                String response = sb.toString();
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
		
	                        final String encryptedProfile = o.getString("profile");
	                        //final String groupSession = o.getString("group_session");
	                        final String idInGroup = o.getString("group_id");
	                        (new Handler(context.getMainLooper())).post(new Runnable(){
	                                public void run(){
	
	                                    ContentValues values = new ContentValues();
	                                    values.put(Contact.PUBLIC_KEY, pubKeyStr);
	
	                                    String profile = "";
	                                    if(encryptedProfile != null && encryptedProfile.length() > 0 && !encryptedProfile.equals("null")) {
	                                        if (DBG) Log.w(TAG, "encrypted profile: ["+encryptedProfile+"]");
	                                        if(key == null) {
	                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
	                                                Log.wtf(TAG, "Null key while handling group request.", new Throwable());
	                                            } else {
	                                                Log.e(TAG, "Null key while handling group request.", new Throwable());
	                                            }
	                                            return;
	                                        }
	                                        profile = "";
	                                        //profile = Util.decryptAES(encryptedProfile, key);
	                                    }
	                                    if(!profile.equals("")) {
	                                        try{
	                                            JSONObject profileJSON = new JSONObject(profile);
	                                            values.put(Contact.NAME, profileJSON.getString("name"));
	                                            if (DBG) Log.w(TAG, "image b64: " + profileJSON.getString("picture"));
	                                            values.put(Contact.PICTURE, FastBase64.decode(profileJSON.getString("picture")));
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
                } finally {
                	ident.close();
                	mHelper.close();
                }
            }
            catch(Exception e){
                Log.e(TAG, "Error in group provider.", e);
            }
        }
    }
}
