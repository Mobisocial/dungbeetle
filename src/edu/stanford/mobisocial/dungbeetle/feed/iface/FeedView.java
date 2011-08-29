package edu.stanford.mobisocial.dungbeetle.feed.iface;

import android.support.v4.app.Fragment;

/**
 * A view on top of a Feed. Don't forget to add your entry to
 * DbViews to make your view selectable.
 *
 */
public interface FeedView {
    public String getName();
	public Fragment getFragment();
}
