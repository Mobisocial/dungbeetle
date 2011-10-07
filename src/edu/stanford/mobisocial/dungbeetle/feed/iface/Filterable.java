package edu.stanford.mobisocial.dungbeetle.feed.iface;

import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import android.support.v4.app.Fragment;

/**
 * A view on top of a Feed. Don't forget to add your entry to
 * DbViews to make your view selectable.
 *
 */
public interface Filterable {
	
    public String[] getFilterTypes();
    public boolean[] getFilterCheckboxes();
    public void setFilterCheckbox(int position, boolean check);
}
