package edu.stanford.mobisocial.dungbeetle.ui.wizard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;

public class WelcomeActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_base);
        
        
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.content);
        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        layout.addView(layoutInflater.inflate(R.layout.wizard_introduction, null, false), 0 );

        ((Button)findViewById(R.id.back_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						WelcomeActivity.this.finish();
					}
    				
    			});
        
        ((Button)findViewById(R.id.next_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

			            Intent setProfile = new Intent(WelcomeActivity.this, SetProfileInstructionsActivity.class);
			            WelcomeActivity.this.startActivity(setProfile);
						//WelcomeActivity.this.finish();
					}
    				
    			});
        

    }
}