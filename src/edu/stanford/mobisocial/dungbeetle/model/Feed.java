package edu.stanford.mobisocial.dungbeetle.model;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import android.graphics.Color;
import android.net.Uri;

public class Feed {
    public static final int BACKGROUND_ALPHA = 150;
    public static Uri uriForName(String name) {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + name);
    }

    public static int colorFor(String name) {
        String feedColorStr = "#444444";
        if (name.length() >= 3) {
            String tmp = name.substring(0, 3);
            StringBuilder doubler = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                doubler.append(tmp.charAt(i)).append(tmp.charAt(i));
            }
            try {
                Integer.parseInt(doubler.toString(), 16);
                feedColorStr = "#" + doubler;
            } catch (NumberFormatException e) {}
        }
        return Color.parseColor(feedColorStr);
    }

    public static int colorFor(String name, int alpha) {
        int c = colorFor(name);
        return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
    }
}
