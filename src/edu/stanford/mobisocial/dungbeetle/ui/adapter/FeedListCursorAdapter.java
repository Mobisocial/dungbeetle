package edu.stanford.mobisocial.dungbeetle.ui.adapter;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.util.ContactCache;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FeedListCursorAdapter extends CursorAdapter {
    private ContactCache mContactCache;

    public FeedListCursorAdapter (Fragment fragment, Cursor cursor) {
        super(fragment.getActivity(), cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        mContactCache = new ContactCache(fragment.getActivity()); // TODO: Global contact cache
        // TODO: does contact cache handle images and attributes?
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.objects_item, parent, false);
        bindView(v, context, c);
        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {
        DbObject.bindView(v, context, c, mContactCache);
    }

    public static CursorLoader queryObjects(Context context) {
        Uri feedList = Feed.uriForList();
        return new CursorLoader(context, feedList, null,
                DbObjects.getFeedObjectClause(), null, DbObject._ID + " DESC");
    }
}