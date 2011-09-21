package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class NewGroupActivity extends Activity {

    
    private DBHelper mHelper;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_group);
        mHelper = new DBHelper(this);

		((Button)findViewById(R.id.newGroupOk)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String groupName = ((EditText)findViewById(R.id.newGroupName)).getText().toString();
                Group g;
                if(groupName.length() > 0) {
                    g = Group.create(NewGroupActivity.this, groupName, mHelper);
                } else {
                    g = Group.create(NewGroupActivity.this);
                }

                Uri feedUri = Feed.uriForName(g.feedName);
                Helpers.sendToFeed(NewGroupActivity.this,
                        StatusObj.from("Welcome to " + g.name + "!"), feedUri);
                Feed.view(NewGroupActivity.this, feedUri);
                NewGroupActivity.this.finish();
            }
        });
        ((Button)findViewById(R.id.newGroupCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NewGroupActivity.this.finish();
            }
        });
        //in case there was an FC, we must restart the service whenever one of our dialogs is opened.
        startService(new Intent(this, DungBeetleService.class));
    }
}
