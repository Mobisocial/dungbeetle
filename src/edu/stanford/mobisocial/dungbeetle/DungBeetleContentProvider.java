package edu.stanford.mobisocial.dungbeetle;
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
        List<String> segs = uri.getPathSegments();
        if(match(uri, "feeds", "me", ".+")){
            try{
                JSONObject obj = new JSONObject(values.getAsString("json"));
                mHelper.addToFeed(
                    mIdent.userPersonId(),
                    "friend",
                    segs.get(2),
                    obj);
                return Uri.parse(uri.toString());
            }
            catch(JSONException e){
                return null;
            }
        }
        else if(match(uri, "contacts")){
            mHelper.insertContact(values);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "subscriptions")){
            mHelper.insertSubscription(values);
            return Uri.parse(uri.toString());
        }
        else if(match(uri, "subscribers")){
            mHelper.insertSubscriber(values);
            return Uri.parse(uri.toString());
        }
        else{
            return null;
        }
    }


    @Override
    public boolean onCreate() {
        mHelper = new DBHelper(getContext());
        mIdent = new DBIdentityProvider(mHelper);
        return mHelper.getWritableDatabase() == null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        List<String> segs = uri.getPathSegments();
        if(match(uri, "feeds", "friend", ".+")){
            return mHelper.queryFeedLatest("friend", segs.get(2));
        }
        else if(match(uri, "feeds", "me", ".+")){
            return mHelper.queryFeedLatest(mIdent.userPersonId(), 
                                           "friend", 
                                           segs.get(2));
        }
        else if(match(uri, "contacts") || 
                match(uri, "subscribers") || 
                match(uri, "subscriptions")){
            return mHelper.getReadableDatabase().query(segs.get(0),
                                                       projection, 
                                                       selection, 
                                                       selectionArgs, 
                                                       null,
                                                       null,
                                                       sortOrder);
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

    // Helper for dispatching on url paths
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

}
