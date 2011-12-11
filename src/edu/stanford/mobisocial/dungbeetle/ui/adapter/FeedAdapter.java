package edu.stanford.mobisocial.dungbeetle.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

public class FeedAdapter extends CursorAdapter {
    public FeedAdapter (Context context, Cursor cursor) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        // TODO: does contact cache handle images and attributes?
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.feed, parent, false);
        bindView(v, context, c);
        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {
        DbObject.bindView(v, context, c, false);
    }

    public static CursorLoader queryObjects(Context context) {
        Uri feedList = Feed.feedListUri();
        return new CursorLoader(context, feedList, null, null, null, null);
    }

    public Uri getFeedUri(int position) {
        mCursor.moveToPosition(position);
        String feedName = mCursor.getString(mCursor.getColumnIndexOrThrow(Feed.FEED_NAME));
        return Feed.uriForName(feedName);
    }
}