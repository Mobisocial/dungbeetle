package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.support.v4.app.Fragment;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedHeadFragment;

// this is what I want the code to look like.
// Views the head of a feed.
public class HeadView implements FeedView {
/*    public View getHeadView(Feed feed) {
        return feed.query().head().view();
        // or feed.query("type=appstate").head().view();
        HeadView.class.
    }

    static {
        new HeadView(); // somehow forces injection into reflection layer.
    }
*/

    @Override
    public String getName() {
        return "Live View";
    }

    @Override
    public Fragment getFragment() {
        // TODO Auto-generated method stub
        return new FeedHeadFragment();
    }
}
