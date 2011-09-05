package edu.stanford.mobisocial.dungbeetle.feed.action;

import org.json.JSONObject;

import android.text.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedAction;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedRefObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class ClipboardAction implements FeedAction {
    private static JSONObject mJson;
    private static String mType;

    @Override
    public String getName() {
        return "Paste";
    }

    public void onClick(Context context, Uri feedUri) {
        Helpers.sendToFeed(context, new DbObject(mType, mJson), feedUri);
        // Clear the clipboard.
        mJson = null;
        mType = null;
    }

    @Override
    public boolean isActive() {
        return mJson != null;
    }

    public static void copyToClipboard(Context context, String type, JSONObject json) {
        mType = type;
        mJson = json;

        if (json.has(StatusObj.TEXT)) {
            ClipboardManager m = (ClipboardManager)context.getSystemService(
                    Context.CLIPBOARD_SERVICE);
            m.setText(json.optString(StatusObj.TEXT));
        }
    }
}
