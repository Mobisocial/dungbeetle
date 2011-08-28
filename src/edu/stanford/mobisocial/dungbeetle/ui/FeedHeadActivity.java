package edu.stanford.mobisocial.dungbeetle.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

public class FeedHeadActivity extends Activity {
    TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);
        mTextView = ((TextView)findViewById(R.id.stats_text));

        Uri feedUri = getIntent().getData();
        // feed.query(selection, selectionArgs);
        Cursor c = getContentResolver().query(feedUri, null, getLocationClause(),
                null, DbObject._ID + " DESC");

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("http://maps.googleapis.com/maps/api/staticmap?size=512x512&sensor=true&path=");
        pathBuilder.append("color:0x0000ff|weight:5");
        int totalSize = c.getCount();
        c.moveToFirst();
        // TODO: This graphs all points in a path.
        // You probably want unique paths for each user.
        for (int i = 1; i < totalSize; i++) {
            c.moveToNext();
            DbObject obj = DbObject.fromCursor(c);
            String lat = obj.getJson().optString(LocationObj.COORD_LAT);
            String lon = obj.getJson().optString(LocationObj.COORD_LONG);
            pathBuilder.append("|").append(lat).append(",").append(lon);
        }
        Uri data = Uri.parse(pathBuilder.toString());
        startActivity(new Intent(Intent.ACTION_VIEW, data));
        finish();
    }

    private String getLocationClause() {
        return DbObject.TYPE + " = '" + LocationObj.TYPE + "'";
    }
}
