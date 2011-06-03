package edu.stanford.mobisocial.dungbeetle.model;

import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import android.net.Uri;

public class Feed {

    public static Uri uriForName(String name) {
        return Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + name);
    }
}
