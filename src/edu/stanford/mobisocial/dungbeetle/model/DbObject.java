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

    protected final String mType;
    protected final JSONObject mJson;

    public DbObject(String type, JSONObject json) {
        mType = type;
        mJson = json;
    }

    public String getType() {
        return mType;
    }
    public JSONObject getJson() {
        return mJson;
    }

    public static DbObject fromCursor(Cursor c) {
        String jsonStr = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        try {
            JSONObject json = new JSONObject(jsonStr);
            return new DbObject(json.optString("type"), json);
        } catch (JSONException e) {
            Log.wtf("Bad json from db", e);
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
