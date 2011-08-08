package edu.stanford.mobisocial.dungbeetle.actions.iface;

import android.content.Context;
import android.net.Uri;

public interface FeedAction {

    public String getName();
    public void onClick(Context context, Uri feedUri);
    public boolean isActive();
}
