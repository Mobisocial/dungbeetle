package edu.stanford.mobisocial.dungbeetle;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.AccessControlException;
import java.util.List;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class DungBeetleContentProvider extends ContentProvider {
	public static final Uri CONTENT_URI = 
        Uri.parse("content://edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider");

    private DataStore dbo;
	private SQLiteDatabase db;

	public DungBeetleContentProvider() {
	}
	@Override
	protected void finalize() throws Throwable {
		if(db != null)
			db.close();
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
        if(match(uri, "me", "profile")){
            try{
                JSONObject obj = new JSONObject(values.getAsString("json"));
                long seqId = dbo.addToFeed(dbo.getMyPersonTag(),
                                           "me",
                                           "profile",
                                           obj);
                return Uri.parse(CONTENT_URI + "/" + "me" + "/" + seqId);
            }
            catch(JSONException e){
                return null;
            }
        }
        else if(match(uri, "me", "status")){
            try{
                JSONObject obj = new JSONObject(values.getAsString("json"));
                long seqId = dbo.addToFeed(dbo.getMyPersonTag(),
                                           "me",
                                           "status",
                                           obj);
                return Uri.parse(CONTENT_URI + "/" + "me" + "/" + seqId);
            }
            catch(JSONException e){
                return null;
            }
        }
        else{
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        dbo = new DataStore(getContext());
        db = dbo.getWritableDatabase();
        return (db == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if(match(uri, "friend", "profile")){
            
        }
        else if(match(uri, "me", "profile")){
            
        }
        else if(match(uri, "me", "status")){
            
        }
        else if(match(uri, "friend", "status")){
            
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
