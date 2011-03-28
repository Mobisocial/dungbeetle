package edu.stanford.mobisocial.dungbeetle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ViewGroupActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String outputText = (String) this.getIntent().getExtras().get("group");
        
        
        TextView textview = new TextView(this);
        textview.setText(outputText);
        
        setContentView(textview);
        
    	/*try {
			   Intent intent = new Intent(Intent.ACTION_CALL);
			   intent.setData(Uri.parse("tel:+436641234567"));
			   startActivity(intent);
		} catch (Exception e) {
			   Log.e("SampleApp", "Failed to invoke call", e);
		}*/
    }
    
    
}