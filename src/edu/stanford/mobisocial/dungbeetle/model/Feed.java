package edu.stanford.mobisocial.dungbeetle.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedRefObj;

public class Feed extends DbObject {
    public static final int BACKGROUND_ALPHA = 150;
    public static final String MIME_TYPE = "vnd.mobisocial.db/feed";

    public static String FEED_NAME_GLOBAL = "global";

    public static Uri uriForName(String name) {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + name);
    }

    public String id() {
        return mJson.optString(FeedRefObj.FEED_ID);
    }
    private static float[] baseHues = new float[] { 21f, 315f };
    public static float[] getBaseHues() {
    	return baseHues;
    }
    public static void setBaseHues(float[] hues) {
    	baseHues = hues;
    }
    public static int colorFor(String name) {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	try {
    		bos.write(name.getBytes());
    	} catch(IOException e) {}
    	SecureRandom r = new SecureRandom(bos.toByteArray());
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
    public static int colorFor(String name, int alpha) {
    	if(name == null)
    		return Color.BLACK;
        int c = colorFor(name);
        return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
    }

    private Feed(Group g) {
        super(FeedRefObj.TYPE, FeedRefObj.json(g));
    }

    public static Feed forGroup(Group g) {
        return new Feed(g);
    }

    public Feed(JSONObject json) {
        super(FeedRefObj.TYPE, json);
    }

    public static Uri uriForList() {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feedlist");
    }

    public static void view(Activity foreground, Uri feedUri) {
        Log.d("MMSSBB",  "viewing " + feedUri);
        Intent launch = new Intent(Intent.ACTION_VIEW);
        launch.setDataAndType(feedUri, MIME_TYPE);
        foreground.startActivity(launch);
    }

    public static final int FEED_GROUP = 1;
    public static final int FEED_FRIEND = 2;
    public static final int FEED_RELATED = 3;
	public static int typeOf(Uri feedUri) {
		if(feedUri.getPath().startsWith("/feeds/friend/")) {
			return FEED_FRIEND;
		} else if (feedUri.getPath().startsWith("/feeds/related/")){
			return FEED_RELATED;
		}
		return FEED_GROUP;
	}
}
