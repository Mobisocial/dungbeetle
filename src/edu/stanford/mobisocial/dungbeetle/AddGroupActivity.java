package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AddGroupActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textview = new TextView(this);
        textview.setText("Add a new group");
        setContentView(textview);
    }
}