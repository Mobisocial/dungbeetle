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
import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;

/**
 * Represents a group by showing its feed and members.
 * TODO: Accept only a group_id extra and query for other parameters.
 */
public class GroupsTabActivity extends TabActivity
{
    private Nfc mNfc;

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
            mNfc.share(NdefFactory.fromUri(intent.getStringExtra("group_uri")));            
        } else if (getIntent().getType().equals(Group.MIME_TYPE)) {
            group_id = Long.parseLong(getIntent().getData().getLastPathSegment());
            Maybe<Group> maybeG = Group.forId(this, group_id);
            try {
                Group g = maybeG.get();
                group_name = g.name;
                feed_name = g.feedName;
            } catch (Exception e) {}
        }

        setTitle("Groups > " + group_name);
        View titleView = getWindow().findViewById(android.R.id.title);
        if (titleView != null) {
            int color = Feed.colorFor(feed_name);
            ViewParent parent = titleView.getParent();
            if (parent != null && parent instanceof View) {
                View parentView = (View) parent;
                parentView.setBackgroundColor(color);
            }
        }
            
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

}





