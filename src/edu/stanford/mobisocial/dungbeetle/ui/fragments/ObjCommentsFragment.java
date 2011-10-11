package edu.stanford.mobisocial.dungbeetle.ui.fragments;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.ui.adapter.ObjectListCursorAdapter;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.Adapter;
import android.widget.Gallery;
import android.widget.SpinnerAdapter;

/**
 * Retreives a cursor from the Content Provider by adding selection constraints:
 * 
 * Musubi notation:
 * --        obj_id = '5098d876effd4af6d7fa0ddf'
 *       ref_obj_id = '2dbe76f6f5df7d6dbf9da765'
 *       feed       = '9fedf7eaaef7cce45deac77a'
 * and relationship = 'child'
 * and         type = 'comment'
 * 
 * EbXML notation:
 *      ref_obj1_id = '5098d876effd4af6d7fa0ddf'
 *      ref_obj2_id = '2dbe76f6f5df7d6dbf9da765'
 * and relationship = 'child'
 * and         type = 'comment'
 */
public class ObjCommentsFragment {

    /**
     * The parametrization here is absolutely not final.
     */
    public static View getViewForObjComments(Activity activity, Uri feedUri, JSONObject obj) {

        Cursor c = activity.getContentResolver().query(feedUri, null,

                DbObjects.getFeedObjectClause(null), null, DbObject._ID + " DESC LIMIT 2");
        try {
	        SpinnerAdapter adapter = new ObjectListCursorAdapter(activity, c);
	
	        Gallery gallery = new Gallery(activity);
	        gallery.setLayoutParams(CommonLayouts.FULL_SCREEN);
	        gallery.setAdapter(adapter);
	
	        return gallery;
        } finally {
        	c.close();
        }
    }
}
