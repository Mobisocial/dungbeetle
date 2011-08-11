package edu.stanford.mobisocial.dungbeetle.feed.iface;

import java.util.LinkedHashSet;

import android.content.Context;
import android.net.Uri;
import android.widget.ListAdapter;

public abstract class FeedProcessor {
    public final LinkedHashSet<Uri> mActiveFeeds = new LinkedHashSet<Uri>();

    public abstract String getName();

    public abstract ListAdapter getListAdapter(Context context, Uri feedUri);
}
