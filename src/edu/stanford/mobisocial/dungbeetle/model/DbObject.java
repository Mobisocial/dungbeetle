package edu.stanford.mobisocial.dungbeetle.model;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.DbUser;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LikeObj;
import edu.stanford.mobisocial.dungbeetle.obj.ObjActions;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.RelativeDate;

/**
 * <p>DO NOT USE AS A REPRESENTATION OF A MUSUBI OBJ.
 * <ul>
 * <li>Obj is an interface for basic Musubi content.
 * <li>MemObj is a concrete implementation stored in memory.
 * <li>SignedObj represents an obj that has been signed for sending by some user.
 * <li>DbObj represents an obj that has been sent or received and is held
 * in Musubi's database.
 * </ul></p>
 * 
 * <p>Note that this class used as both a representation of Objs, and a set of
 * utility methods and constants. Only the use as an Obj is deprecated,
 * the rest will be moved to a new class.</p>
 */
public class DbObject implements Obj {
    private static final String TAG = "dbObject";
    private static final boolean DBG = true;

    public static final String TABLE = DbObj.TABLE;
    public static final String _ID = DbObj.COL_ID;
    public static final String TYPE = DbObj.COL_TYPE;
    public static final String SEQUENCE_ID = DbObj.COL_SEQUENCE_ID;
    public static final String FEED_NAME = DbObj.COL_FEED_NAME;
    public static final String CONTACT_ID = DbObj.COL_CONTACT_ID;
    public static final String DESTINATION = DbObj.COL_DESTINATION;
    public static final String JSON = DbObj.COL_JSON;
    public static final String TIMESTAMP = DbObj.COL_TIMESTAMP;
    public static final String LAST_MODIFIED_TIMESTAMP = DbObj.COL_LAST_MODIFIED_TIMESTAMP;
    public static final String APP_ID = DbObj.COL_APP_ID;
    public static final String SENT = DbObj.COL_SENT;
	public static final String ENCODED = DbObj.COL_ENCODED;
	public static final String CHILD_FEED_NAME = DbObj.COL_CHILD_FEED_NAME;
	public static final String HASH = DbObj.COL_HASH;
	public static final String DELETED = DbObj.COL_DELETED;
	public static final String RAW = DbObj.COL_RAW;
	public static final String KEY_INT = DbObj.COL_KEY_INT;

	protected final String mType;
    protected JSONObject mJson;
    protected byte[] mRaw;
    protected Integer mIntKey;

    private static OnClickViewProfile sViewProfileAction;
    private static final int sDeletedColor = Color.parseColor("#66FF3333");

    /**
     * Use SocialKit Obj implementations.
     */
    @Deprecated
    public DbObject(String type, JSONObject json, byte[] raw) {
        mType = type;
        mJson = json;
        mRaw = raw;
    }

    /**
     * Use SocialKit Obj implementations.
     */
    @Deprecated
    public DbObject(String type, JSONObject json) {
        mType = type;
        mJson = json;
        mRaw = null;
    }

    /**
     * Use SocialKit Obj implementations.
     */
    @Deprecated
    private DbObject(Cursor c) {
        mType = c.getString(c.getColumnIndexOrThrow(DbObject.TYPE));
        String jsonStr = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
        try {
            mRaw = c.getBlob(c.getColumnIndexOrThrow(DbObject.RAW));
        } catch (IllegalArgumentException e) {
            mRaw = null;
        }
        try {
            mJson = new JSONObject(jsonStr);
        } catch (JSONException e) {
            Log.wtf("DB", "Bad json from database.");
        }
        //mTimestamp = c.getLong(c.getColumnIndexOrThrow(DbObject.TIMESTAMP));
    }

    public String getType() {
        return mType;
    }
    public JSONObject getJson() {
        return mJson;
    }

    /**
     * Use SocialKit Obj implementations.
     */
    @Deprecated
    public static DbObject fromCursor(Cursor c) {
        try {
            return new DbObject(c);
        } catch (Exception e) {
            Log.wtf("Bad data from db", e);
            return null;
        } 
    }

    public static final Uri OBJ_URI = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/obj");

    public static Uri uriForObj(long objId) {
        return OBJ_URI.buildUpon().appendPath("" + objId).build();
    }

    /**
     * @param v the view to bind
     * @param context standard activity context
     * @param c the cursor source for the object in the db object table.
     * Must include _id in the projection.
     * 
     * @param allowInteractions controls whether the bound view is
     * allowed to intercept touch events and do its own processing.
     */
    public static void bindView(View v, final Context context, Cursor cursor, boolean allowInteractions) {
    	TextView nameText = (TextView) v.findViewById(R.id.name_text);
        ViewGroup frame = (ViewGroup)v.findViewById(R.id.object_content);
        frame.removeAllViews();

        // make sure we have all the columns we need
        Long objId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_ID));
        String[] projection = null;
        String selection = DbObj.COL_ID + " = ?";
        String[] selectionArgs = new String[] { Long.toString(objId) };
        String sortOrder = null;
        Cursor c = context.getContentResolver().query(DbObj.OBJ_URI, projection, selection, selectionArgs, sortOrder);
        if (!c.moveToFirst()) {
            Log.w(TAG, "could not find obj " + objId);
            c.close();
            return;
        }
        DbObj obj = App.instance().getMusubi().objForCursor(c);
        if (obj == null) {
            nameText.setText("Failed to access database.");
            Log.e("DbObject", "cursor was null for bindView of DbObject");
            return;
        }
        DbUser sender = obj.getSender();
        Long timestamp = c.getLong(c.getColumnIndexOrThrow(DbObj.COL_TIMESTAMP));
        Long hash = obj.getHash();
        short deleted = c.getShort(c.getColumnIndexOrThrow(DELETED));
        String feedName = obj.getFeedName();
        String type = obj.getType();
        Date date = new Date(timestamp);
        c.close();
        c = null;

        if (sender == null) {
            nameText.setText("Message from unknown contact.");
            return;
        }
        nameText.setText(sender.getName());

        final ImageView icon = (ImageView)v.findViewById(R.id.icon);
        if (sViewProfileAction == null) {
            sViewProfileAction = new OnClickViewProfile((Activity)context);
        }
        icon.setTag(sender.getLocalId());
        if (allowInteractions) {
            icon.setOnClickListener(sViewProfileAction);
            v.setTag(objId);
        }
        icon.setImageBitmap(sender.getPicture());

        if (deleted == 1) {
            v.setBackgroundColor(sDeletedColor);
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView timeText = (TextView)v.findViewById(R.id.time_text);
        timeText.setText(RelativeDate.getRelativeDate(date));

        frame.setTag(objId); // TODO: error prone! This is database id
        frame.setTag(R.id.object_entry, cursor.getPosition()); // this is cursor id
        FeedRenderer renderer = DbObjects.getFeedRenderer(type);
		if(renderer != null) {
			renderer.render(context, frame, obj, allowInteractions);
		}

        if (!allowInteractions) {
            v.findViewById(R.id.obj_attachments_icon).setVisibility(View.GONE);
            v.findViewById(R.id.obj_attachments).setVisibility(View.GONE);
        } else {
            if (!MusubiBaseActivity.isDeveloperModeEnabled(context)){
                v.findViewById(R.id.obj_attachments_icon).setVisibility(View.GONE);
                v.findViewById(R.id.obj_attachments).setVisibility(View.GONE);
            } else {
                ImageView attachmentCountButton = (ImageView)v.findViewById(R.id.obj_attachments_icon);
                TextView attachmentCountText = (TextView)v.findViewById(R.id.obj_attachments);
                attachmentCountButton.setVisibility(View.VISIBLE);

                if (hash == 0) {
                    attachmentCountButton.setVisibility(View.GONE);
                } else {
                    //int color = DbObject.colorFor(hash);
                    boolean selfPost = false;
                    DBHelper helper = new DBHelper(context);
                    try {
                        Cursor attachments = obj.getSubfeed()
                                .query("type=?", new String[] { LikeObj.TYPE });
                        try {
                            attachmentCountText.setText("+" + attachments.getCount());
                            
                            if(attachments.moveToFirst()) {
	                            while (!attachments.isAfterLast()) {
	                            	if (attachments.getInt(attachments.getColumnIndex(CONTACT_ID)) == -666) {
	                            		selfPost = true;
	                            		break;
	                            	}
	                            	attachments.moveToNext();
	                            	
	                            }
                            }
                        } finally {
                        	attachments.close();
                        }
                    } finally {
                        helper.close();
                    }
                    if (selfPost) {
                    	attachmentCountButton.setImageResource(R.drawable.ic_menu_love_red);
                    }
                    else {
                    	attachmentCountButton.setImageResource(R.drawable.ic_menu_love);
                    }
                    attachmentCountText.setTag(R.id.object_entry, hash);
                    attachmentCountText.setTag(R.id.feed_label, Feed.uriForName(feedName));
                    attachmentCountText.setOnClickListener(LikeListener.getInstance(context));
                }
            }
        }
    }

    private static int colorFor(Long hash) {
        float[] baseHues = Feed.getBaseHues();
        ByteBuffer bos = ByteBuffer.allocate(8);
        bos.putLong(hash);
        byte[] hashBytes = new byte[8];
        bos.position(0);
        bos.get(hashBytes);
        SecureRandom r = new SecureRandom(hashBytes);
        float hsv[] = new float[] {
                baseHues[r.nextInt(baseHues.length)],
                r.nextFloat(),
                r.nextFloat()
        };
        hsv[0] = hsv[0] + 20 * r.nextFloat() - 10; 
        hsv[1] = hsv[1] * 0.2f + 0.8f;
        hsv[2] = hsv[2] * 0.2f + 0.8f;
        return Color.HSVToColor(hsv);
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

    private static class LikeListener implements OnClickListener {
        private Context mmContext;
        private static LikeListener mmListener;
        public static LikeListener getInstance(Context context) {
            if (mmListener == null || mmListener.mmContext != context) {
                mmListener = new LikeListener(context);
            }
            return mmListener;
        }

        private LikeListener(Context context) {
            mmContext = context;
        }

        @Override
        public void onClick(View v) {
            Long hash = (Long)v.getTag(R.id.object_entry);
            Uri feed = (Uri)v.getTag(R.id.feed_label);
            String label = ((TextView)v).getText().toString();
            DbObject obj = LikeObj.forObj(hash, label);
            Helpers.sendToFeed(mmContext, obj, feed);
        }
    };

    public static class ItemClickListener implements View.OnClickListener {
        private final Context mContext;
        private static ItemClickListener sInstance;

        private ItemClickListener(Context context) {
            mContext = context;
        }

        public static ItemClickListener getInstance(Context context) {
            if (sInstance == null || sInstance.mContext != context) {
                sInstance = new ItemClickListener(context);
            }
            return sInstance;
        }
       
        @Override
        public void onClick(View v) {
            Object tag = v.getTag();
            if (tag == null || !(tag instanceof Long)) {
                Log.d(TAG, "no id for dbobj " + v + "; parent: " + v.getParent());
                return;
            }
            long objId = (Long)tag;

            SignedObj obj = App.instance().getMusubi().objForId(objId);
            if (HomeActivity.DBG) Log.i(TAG, "Clicked object: " + obj.getRaw() + ", " + obj.getJson());
            Activator activator = DbObjects.getActivator(obj.getType());
            activator.activate(mContext, obj);
        }
    };

    public static class ItemLongClickListener implements View.OnLongClickListener {
        private final Context mContext;
        private static ItemLongClickListener sInstance;

        private ItemLongClickListener(Context context) {
            mContext = context;
        }

        public static ItemLongClickListener getInstance(Context context) {
            if (sInstance == null || sInstance.mContext != context) {
                sInstance = new ItemLongClickListener(context);
            }
            return sInstance;
        }
       
        @Override
        public boolean onLongClick(View v) {
            if (v == null || v.getTag() == null) {
                Log.d(TAG, "missing objId for " + v);
                return false;
            }
            long objId = (Long)v.getTag();
            DbObj obj = App.instance().getMusubi().objForId(objId);
            createActionDialog(mContext, obj).show();
            return false;
        }
    };

    public static Dialog createActionDialog(final Context context, final DbObj obj) {

        final DbEntryHandler dbType = DbObjects.forType(obj.getType());
        final List<ObjAction> actions = new ArrayList<ObjAction>();
        for (ObjAction action : ObjActions.getObjActions()) {
            if (action.isActive(context, dbType, obj.getJson())) {
                actions.add(action);
            }
        }
        final String[] actionLabels = new String[actions.size()];
        int i = 0;
        for (ObjAction action : actions) {
            actionLabels[i++] = action.getLabel(context);
        }
        return new AlertDialog.Builder(context).setTitle("Handle...")
                .setItems(actionLabels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actions.get(which).actOn(context, dbType, obj);
                    }
                }).create();
    }

    @Override
    public byte[] getRaw() {
        return mRaw;
    }

    @Override
    public Integer getInt() {
        return mIntKey;
    }
}
