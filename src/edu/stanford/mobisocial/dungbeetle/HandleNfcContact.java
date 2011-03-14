package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;


public class HandleNfcContact extends Activity {
	public final static String LAUNCH_INTENT = "edu.stanford.mobisocial.dungbeetle.HANDLE_NFC_CONTACT";
	private TextView mNameText;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setContentView(R.layout.handle_give);
		// Intent intent = getIntent();
		// final String scheme=intent.getScheme();
		// if(scheme != null && scheme.equals(DungBeetleActivity.SHARE_CONTACT_INFO_SCHEME)){
		// 	// final Uri myURI=intent.getData();
		// 	// if(myURI!=null){
		// 	// 	String seed = myURI.getQueryParameter("seed");
		// 	// 	mCreature = new Creature(seed);
		// 	// 	CreatureStore.addCreatureToStore(this, mCreature);
		// 	// 	Toast.makeText(this, "You've received a new monster!", Toast.LENGTH_SHORT).show();
		// 	// }
		// 	// else{
		// 	// 	Toast.makeText(this, "Received null url...", Toast.LENGTH_SHORT).show();
		// 	// }
		// }
		// else{
		// 	Toast.makeText(this, "Failed to receive monster :(", Toast.LENGTH_SHORT).show();
		// }

		// if(mCreature != null){
		// 	mNameText = (TextView)findViewById(R.id.name_text);
		// 	mNameText.setText("Name: " + mCreature.name());
		// }

		Button button = (Button)findViewById(R.id.finished_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});

	}

}



