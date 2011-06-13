package edu.stanford.mobisocial.dungbeetle;

import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.objects.StatusObj;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NewFeedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Group g = Group.create(context);
        Helpers.sendToFeed(context,
                StatusObj.from("Welcome to " + g.name + "!"), Feed.uriForName(g.feedName));
        setResultData("content://vnd.mobisocial.db/feed/" + g.feedName);
        getResultExtras(true).putString("download", "http://openjunction.org/demos/jxwhiteboard.apk");
    }
}
