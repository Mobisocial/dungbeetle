package edu.stanford.mobisocial.dungbeetle.model;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LikeObj;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.RelativeDate;

public class DbObject {
    public static final String TABLE = "objects";
    public static final String _ID = "_id";
    public static final String TYPE = "type";
    public static final String SEQUENCE_ID = "sequence_id";
    public static final String FEED_NAME = "feed_name";
    public static final String CONTACT_ID = "contact_id";
    public static final String DESTINATION = "destination";
    public static final String JSON = "json";
    public static final String TIMESTAMP = "timestamp";
    public static final String APP_ID = "app_id";
    public static final String SENT = "sent";
	public static final String ENCODED = "encoded";
	public static final String CHILD_FEED_NAME = "child_feed";
	public static final String HASH = "hash";

    public static final String EXTRA_FEED_URI = "feed_uri";
	public static final String RAW = "raw";

    private final Cursor mCursor;
    protected final String mType;
    protected JSONObject mJson;
    private Long mTimestamp;
    private static OnClickViewProfile sViewProfileAction;

    public DbObject(String type, JSONObject json) {
        mCursor = null;
        mType = type;
        mJson = json;
    }

    private DbObject(Cursor c) {
        mType = c.getString(c.getColumnIndexOrThrow(DbObject.TYPE));
        mCursor = c;
    }

    public String getType() {
        return mType;
    }
    public JSONObject getJson() {
        if (mJson == null && mCursor != null) {
            String jsonStr = mCursor.getString(mCursor.getColumnIndexOrThrow(DbObject.JSON));
            try {
                mJson = new JSONObject(jsonStr);
            } catch (JSONException e) {
                Log.wtf("DB", "Bad json from database.");
            }
        }
        return mJson;
    }

    public Long getTimestamp() {
        if (mTimestamp == null && mCursor != null) {
            mTimestamp = mCursor.getLong(mCursor.getColumnIndexOrThrow(DbObject.TIMESTAMP));
        }
        return mTimestamp;
    }

    public static DbObject fromCursor(Cursor c) {
        try {
            return new DbObject(c);
        } catch (Exception e) {
            Log.wtf("Bad data from db", e);
            return null;
        } 
    }
    public static final Uri OBJ_URI = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/obj");
    /**
     * @param v the view to bind
     * @param context standard activity context
     * @param c the cursor source for the object in the db object table
     * @param contactCache prevents copious lookups of contact information from the sqlite database
     * @param allowInteractions controls whether the bound view is allowed to intercept touch events and do its own processing.
     */
    public static void bindView(View v, Context context, Cursor c, ContactCache contactCache, boolean allowInteractions) {
    	//there is probably a utility or should be one that does this
        long objId = c.getLong(0);
    	Cursor cursor = context.getContentResolver().query(OBJ_URI,
            	new String[] { 
            		DbObject.JSON,
            		DbObject.RAW,
            		DbObject.CONTACT_ID,
            		DbObject.TIMESTAMP,
            		DbObject.HASH
            	},
            	DbObject._ID + " = ?", new String[] {String.valueOf(objId)}, null);
    	if(cursor == null) {
    		Log.wtf("Dbbject", "cursor was null for bund view of db object");
    		return;
    	}
        if(!cursor.moveToFirst())
        	return;
        
        String jsonSrc = cursor.getString(0);
        byte[] raw = cursor.getBlob(1);
        Long contactId = cursor.getLong(2);
        Long timestamp = cursor.getLong(3);
        Long hash = cursor.getLong(4);
        Date date = new Date(timestamp);
        cursor.close();
       	///////
        
        try{
            Contact contact = contactCache.getContact(contactId).get();

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(contact.name);

            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            if (sViewProfileAction == null) {
                sViewProfileAction = new OnClickViewProfile((Activity)context);
            }
            icon.setTag(contactId);
            if (allowInteractions) {
                icon.setOnClickListener(sViewProfileAction);
            }
            // TODO: this is horrible
            ((App)((Activity)context).getApplication()).contactImages.lazyLoadContactPortrait(contact, icon);

            try {
                JSONObject content = new JSONObject(jsonSrc);

                TextView timeText = (TextView)v.findViewById(R.id.time_text);
                timeText.setText(RelativeDate.getRelativeDate(date));

                ViewGroup frame = (ViewGroup)v.findViewById(R.id.object_content);
                frame.removeAllViews();
                frame.setTag(R.id.object_entry, c.getPosition());
                
        		FeedRenderer renderer = DbObjects.getFeedRenderer(content);
        		if(renderer != null)
        			renderer.render(context, frame, content, raw, allowInteractions);

                if (!allowInteractions) {
                    v.findViewById(R.id.obj_attachments).setVisibility(View.GONE);
                } else {
                    /*
                     * Set the number of current likes 
                     * TODO: Optimize
                     */
                    DBHelper helper = new DBHelper(context);
                    Cursor likes = helper.queryRelatedObjs(objId, LikeObj.TYPE);
                    Button button = (Button)v.findViewById(R.id.obj_attachments);
                    button.setText("Likes: " + likes.getCount());
                    helper.close();
                    button.setTag(hash);
                    button.setOnClickListener(mLikeListener);
                }
            } catch (JSONException e) {
                Log.e("db", "error opening json");
            }
        }
        catch(Maybe.NoValError e){}
    }

    public static class OnClickViewProfile implements View.OnClickListener {
        private final Activity mmContext;

        @Override
        public void onClick(View v) {
            Long contactId = (Long)v.getTag();
            Contact.view(mmContext, contactId);
        }

        public OnClickViewProfile(Activity c) {
            mmContext = c;
        }
    }

    private static OnClickListener mLikeListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Long hash = (Long)v.getTag();
            Log.d("musubi", "LIKED " + hash);
        }
    };
}
