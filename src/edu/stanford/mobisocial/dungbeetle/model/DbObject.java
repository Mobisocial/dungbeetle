package edu.stanford.mobisocial.dungbeetle.model;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
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

    public static final String EXTRA_FEED_URI = "feed_uri";

    private final Cursor mCursor;
    protected final String mType;
    protected JSONObject mJson;
    private Long mTimestamp;

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
    public static void bindView(View v, Context context, Cursor c, ContactCache contactCache) {
        String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        Long contactId = c.getLong(c.getColumnIndexOrThrow(DbObject.CONTACT_ID));
        Long timestamp = c.getLong(c.getColumnIndexOrThrow(DbObject.TIMESTAMP));
        Date date = new Date(timestamp);
        try{
            Contact contact = contactCache.getContact(contactId).get();

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(contact.name);

            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
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
                if(renderer != null){
                    renderer.render(context, frame, content);
                }
            } catch (JSONException e) {
                Log.e("db", "error opening json");
            }
        }
        catch(Maybe.NoValError e){}
    }
}
