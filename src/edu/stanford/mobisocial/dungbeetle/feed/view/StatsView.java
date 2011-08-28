package edu.stanford.mobisocial.dungbeetle.feed.view;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.processor.DefaultFeedProcessor;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class StatsView extends Activity {
    TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);
        mTextView = ((TextView)findViewById(R.id.stats_text));

        Uri feedUri = getIntent().getData();
        Cursor c = getContentResolver().query(feedUri, null, DefaultFeedProcessor.getFeedObjectClause(),
                null, DbObject._ID + " DESC");

        Map<String, Integer> counts = new HashMap<String, Integer>();
        int totalSize = c.getCount();
        c.moveToFirst();
        for (int i = 1; i < totalSize; i++) {
            c.moveToNext();
            DbObject obj = DbObject.fromCursor(c);
            String type = obj.getType();
            if (counts.containsKey(type)) {
                counts.put(type, counts.get(type) + 1);
            } else {
                counts.put(type, 1);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (String type : counts.keySet()) {
            builder.append(type + ": " + counts.get(type)).append("\n");
        }
        mTextView.setText(builder.toString());
    }
}
