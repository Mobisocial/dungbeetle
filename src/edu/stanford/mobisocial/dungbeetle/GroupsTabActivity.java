package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.widget.TabHost;
import android.widget.Toast;
import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;
import android.widget.TextView;
import android.util.Log;
import android.content.Context;

/**
 * Represents a group by showing its feed and members.
 * TODO: Accept only a group_id extra and query for other parameters.
 */
public class GroupsTabActivity extends TabActivity
{
    private Nfc mNfc;

    public final String TAG = "GroupsTabActivity";

/*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, DungBeetleActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId, String title)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (title);
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }


    public void onClickSearch (View v)
    {
        startActivity (new Intent(getApplicationContext(), SearchActivity.class));
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mNfc = new Nfc(this);

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  

        Intent intent = getIntent();
        Long group_id = null;
        String group_name = null;
        String feed_name = null;
        String feed_uri = null;
        // TODO: Depracate extras-based access in favor of Data field.
        if (intent.hasExtra("group_id")) {
            group_id = intent.getLongExtra("group_id", -1);
            group_name = intent.getStringExtra("group_name");
            feed_name = group_name;
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                feed_name = g.feedName;
            } catch (Exception e) {}
            feed_uri = intent.getStringExtra("group_uri");
        } else if (getIntent().getType() != null && getIntent().getType().equals(Group.MIME_TYPE)) {
            group_id = Long.parseLong(getIntent().getData().getLastPathSegment());
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                group_name = g.name;
                feed_name = g.feedName;
                feed_uri = g.dynUpdateUri;
            } catch (Exception e) {}
        } else if (getIntent().getData().getAuthority().equals("vnd.mobisocial.db")) {
            String feedName = getIntent().getData().getLastPathSegment();
            Maybe<Group>maybeG = Group.forFeed(this, feedName);
            Group g = null;
            try {
               g = maybeG.get();
                
            } catch (Exception e) {
                g = Group.createForFeed(this, feedName);
            }
            group_name = g.name;
            feed_name = g.feedName;
            feed_uri = g.dynUpdateUri;
            group_id = g.id;
        }

        if (feed_uri != null) {
            mNfc.share(NdefFactory.fromUri(feed_uri));
            Log.w(TAG, feed_uri);
        }

        int color = Feed.colorFor(feed_name);
        
        setTitleFromActivityLabel (R.id.title_text, group_name);
        View titleView = getWindow().findViewById(android.R.id.title);
        if (titleView != null) {
            ViewParent parent = titleView.getParent();
            if (parent != null && parent instanceof View) {
                View parentView = (View) parent;
                parentView.setBackgroundColor(color);
            }
        }

        // Note: If you change this color, also update the cacheColorHint
        // in FeedActivity and ContactsActivity.
        //tabHost.setBackgroundColor(color);
        //tabHost.getBackground().setAlpha(Feed.BACKGROUND_ALPHA);
            
        intent = new Intent().setClass(this, FeedActivity.class);
        intent.putExtra("group_id", group_id);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feed",
            null).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, ContactsActivity.class);
        intent.putExtra("group_id", group_id);
        intent.putExtra("group_name", group_name);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Members",
            null).setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfc.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mNfc.onNewIntent(this, intent)) return;
    }

    public void toast(final String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}





