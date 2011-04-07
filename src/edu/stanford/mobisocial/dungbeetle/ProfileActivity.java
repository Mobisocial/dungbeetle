package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ProfileActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile);

		final DBHelper helper = new DBHelper(ProfileActivity.this);
        
        final EditText profile_name = (EditText) findViewById(R.id.profile_name);
        
        profile_name.setText(helper.getMyName());
        
        Button save_button = (Button) findViewById(R.id.save_profile_button);
        
        save_button.setOnClickListener(new OnClickListener(){
            	public void onClick(View v)
            	{
            		helper.setMyName(profile_name.getText().toString());
            		Helpers.updateProfile(ProfileActivity.this, profile_name.getText().toString());
            	}
        });
        
        
        
        
    }
}
