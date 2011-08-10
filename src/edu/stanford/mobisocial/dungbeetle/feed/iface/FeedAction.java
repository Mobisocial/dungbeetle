package edu.stanford.mobisocial.dungbeetle.feed.iface;

import android.content.Context;
import android.net.Uri;

public interface FeedAction {

    public String getName();
    public void onClick(Context context, Uri feedUri);
    public boolean isActive();
}
