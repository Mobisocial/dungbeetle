package edu.stanford.mobisocial.dungbeetle.objects;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public class ProfileHandler extends MessageHandler {
	public static final String TAG = "db";

	public ProfileHandler(Context c) {
		super(c);
	}

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals("profile");
	}

	public void handleReceived(Contact from, JSONObject obj) {
		String name = obj.optString("name");
		String id = Long.toString(from.id);

		Log.i(TAG, "Updating " + id + " name=" + name);
		ContentValues values = new ContentValues();
		values.put(Contact.NAME, name);
		mContext.getContentResolver().update(
				Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
				values, "_id=?", new String[] { id });
	}
}