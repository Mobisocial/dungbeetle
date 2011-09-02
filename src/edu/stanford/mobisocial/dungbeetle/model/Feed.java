package edu.stanford.mobisocial.dungbeetle.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.json.JSONObject;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.HorizontalScrollView;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.objects.FeedRefObj;

public class Feed extends DbObject {
    public static final int BACKGROUND_ALPHA = 150;
    public static final String MIME_TYPE = "vnd.mobisocial.db/feed";

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
}
