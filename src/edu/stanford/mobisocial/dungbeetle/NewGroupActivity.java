package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.widget.EditText;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.objects.StatusObj;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.FeedHomeActivity;

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
                        }
                        else {
                            g = Group.create(NewGroupActivity.this);
                        }
                        
                        Helpers.sendToFeed(NewGroupActivity.this,
                        StatusObj.from("Welcome to " + g.name + "!"), Feed.uriForName(g.feedName));

                        Intent launch = new Intent();
                        launch.setClass(NewGroupActivity.this, FeedHomeActivity.class);
                        launch.putExtra("group_name", g.name);
                        launch.putExtra("group_id", g.id);
                        launch.putExtra("group_uri", g.dynUpdateUri);
                        startActivity(launch);
                        NewGroupActivity.this.finish();
                    }
                });
        ((Button)findViewById(R.id.newGroupCancel)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        NewGroupActivity.this.finish();
                    }
                });
    }

}
