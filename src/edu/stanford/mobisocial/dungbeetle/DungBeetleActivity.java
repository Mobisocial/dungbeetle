package edu.stanford.mobisocial.dungbeetle;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;

public class DungBeetleActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startService(new Intent(this, DungBeetleService.class));
    }

}
