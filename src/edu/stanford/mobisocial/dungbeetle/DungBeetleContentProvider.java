package edu.stanford.mobisocial.dungbeetle;
import android.widget.Toast;
import android.app.Activity;
import android.app.ActivityManager;
import android.os.Binder;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class DungBeetleContentProvider extends ContentProvider {
	public static final String AUTHORITY = 
        "edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	static final String TAG = "DungBeetleContentProvider";
	private static final String SUPER_APP_ID = "edu.stanford.mobisocial.dungbeetle";

    private DBHelper mHelper;
    private DBIdentityProvider mIdent;

	public DungBeetleContentProvider() {}

	@Override
	protected void finalize() throws Throwable {
        mHelper.close();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		List<String> segs = uri.getPathSegments();
		if (segs.size() == 3){
            return "vnd.android.cursor.item/vnd.dungbeetle.feed";
        }
		else if (segs.size() == 2){
            return "vnd.android.cursor.dir/vnd.dungbeetle.feed";
        }
        else{
			throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

        final String appId = getCallingActivityId();
        if(appId == null){
            return null;
        }

        List<String> segs = uri.getPathSegments();
        if(match(uri, "feeds", "me")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            try{
                mHelper.addToFeed(
                    appId,
                    mIdent.userPersonId(),
                    "friend",
                    values.getAsString("type"),
                    new JSONObject(values.getAsString("json")));
                getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/feeds/me"), null);
                getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/feeds/friend"), null);
                return Uri.parse(uri.toString());
            }
            catch(JSONException e){
                return null;
            }
        }
        else if(match(uri, "out")){
            try{
                JSONObject obj = new JSONObject(values.getAsString("json"));
                mHelper.addToOutgoing(
                    appId,
                    mIdent.userPersonId(),
                    values.getAsString("to_person_id"),
                    values.getAsString("type"),
                    obj);
                getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/out"), null);
                return Uri.parse(uri.toString());
            }
            catch(JSONException e){
                return null;
            }
        }
        else if(match(uri, "contacts")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            mHelper.insertContact(values);
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/contacts"), null);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "subscriptions")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            mHelper.insertSubscription(values);
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/subscriptions"), null);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "subscribers")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            mHelper.insertSubscriber(values);
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/subscribers"), null);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "groups")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            mHelper.insertGroup(values);
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/groups"), null);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "group_members")){
            if(!appId.equals(SUPER_APP_ID)) return null;
            mHelper.insertGroupMember(values);
            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/group_members"), null);
            return Uri.parse(uri.toString());
        }
        else{
            return null;
        }
    }


    @Override
    public boolean onCreate() {
        Log.i(TAG, "Creating DungBeetleContentProvider");
        mHelper = new DBHelper(getContext());
        mIdent = new DBIdentityProvider(mHelper);
        return mHelper.getWritableDatabase() == null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        final String appId = getCallingActivityId();
        if(appId == null){
            return null;
        }

        List<String> segs = uri.getPathSegments();
        if(match(uri, "feeds", ".+")){
            boolean isMe = segs.get(1).equals("me");
            String feedName = isMe ? "friend" : segs.get(1);
            String select = isMe ? DBHelper.andClauses(selection, "person_id='" + mIdent.userPersonId() + "'") : selection;
            Cursor c = mHelper.queryFeed(appId,
                                         feedName,
                                         projection,
                                         select,
                                         selectionArgs,
                                         sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), Uri.parse(CONTENT_URI + "/feeds/" + feedName));
            if(isMe) c.setNotificationUri(getContext().getContentResolver(), Uri.parse(CONTENT_URI + "/feeds/me"));
            return c;
        }
        else if(match(uri, "feeds", ".+", "head")){
            boolean isMe = segs.get(1).equals("me");
            String feedName = isMe ? "friend" : segs.get(1);
            String select = isMe ? DBHelper.andClauses(selection, "person_id='" + mIdent.userPersonId() + "'") : selection;
            Cursor c = mHelper.queryFeedLatest(appId,
                                               feedName,
                                               projection,
                                               select,
                                               selectionArgs,
                                               sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), Uri.parse(CONTENT_URI + "/feeds/" + feedName));
            if(isMe) c.setNotificationUri(getContext().getContentResolver(), Uri.parse(CONTENT_URI + "/feeds/me"));
            return c;
        }
        else if(match(uri, "contacts") || 
                match(uri, "subscribers") || 
                match(uri, "subscriptions") ||
                match(uri, "groups") ||
                match(uri, "group_members")){

            if(!appId.equals(SUPER_APP_ID)) return null;

            Cursor c = mHelper.getReadableDatabase().query(segs.get(0),
                                                           projection,
                                                           selection,
                                                           selectionArgs,
                                                           null,
                                                           null,
                                                           sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), 
                                 Uri.parse(CONTENT_URI + "/" + segs.get(0)));
            return c;
        }
        else{
            return null;
        }
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    // For unit tests
    public DBHelper getDatabaseHelper(){
        return mHelper;
    }

    // Helper for matching on url paths
    private boolean match(Uri uri, String... regexes){
        List<String> segs = uri.getPathSegments();
        if(segs.size() == regexes.length){
            for(int i = 0; i < regexes.length; i++){
                if(!segs.get(i).matches(regexes[i])){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String getCallingActivityId(){
        int pid = Binder.getCallingPid();

        ActivityManager am = (ActivityManager) 
            getContext().getSystemService(Activity.ACTIVITY_SERVICE); 

        List<ActivityManager.RunningAppProcessInfo> lstAppInfo = 
            am.getRunningAppProcesses();

        for(ActivityManager.RunningAppProcessInfo ai : lstAppInfo) { 
            if (ai.pid == pid) {
                return ai.processName;
            } 
        } 
        return null; 
    }

    

}
