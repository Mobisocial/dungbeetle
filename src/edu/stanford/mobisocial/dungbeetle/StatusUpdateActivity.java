package edu.stanford.mobisocial.dungbeetle;

import mobisocial.nfc.NdefFactory;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class StatusUpdateActivity extends Activity {
	private static final String TAG = "dbupdatestatus";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_update);

        buildUi();
    }
    
    private void buildUi() {
    	findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String update = ((EditText)findViewById(R.id.status)).getText().toString();
				Intent result = new Intent();
				result.putExtra(Intent.EXTRA_TEXT, update);
				setResult(RESULT_OK, result);
				finish();
			}
		});
    }
	
	private void toast(final String text) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(StatusUpdateActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
}