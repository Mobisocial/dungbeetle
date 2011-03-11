package edu.stanford.mobisocial.dungbeetle;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import android.content.ContentProvider;
import android.content.ContentValues;
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
        List<String> segs = uri.getPathSegments();
        if(match(uri, "me", ".+")){
            try{
                JSONObject obj = new JSONObject(values.getAsString("json"));
                dbo.addToFeed(
                    dbo.getMyCreatorTag(),
                    "friend",
                    segs.get(1),
                    obj);
                return Uri.parse(CONTENT_URI + "/" + "me" + "/" + segs.get(1));
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
        List<String> segs = uri.getPathSegments();
        if(match(uri, "friend", ".+")){
            return dbo.queryLatest("friend", segs.get(1));
        }
        else if(match(uri, "me", ".+")){
            return dbo.queryLatest(dbo.getMyCreatorTag(), 
                                   "friend", segs.get(1));
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
