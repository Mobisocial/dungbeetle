package edu.stanford.mobisocial.dungbeetle;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.view.View;
import android.content.Intent;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.content.Context;
import android.util.Log;
import android.content.Intent;



public class SettingsActivity extends ListActivity {
    String[] listItems = {"Backup to Dropbox", "Restore from Dropbox", "Wipe Data (Keep identity)", "Start from Scratch"};
    String TAG = "Settings";


/*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, DungBeetleActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (getTitle ());
    } 
    public void onClickHome (View v)
    {
        goHome (this);
    }


    public void onClickSearch (View v)
    {
        startActivity (new Intent(getApplicationContext(), SearchActivity.class));
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setTitleFromActivityLabel (R.id.title_text);
        setListAdapter(new ArrayAdapter(this, 
        android.R.layout.simple_list_item_1, listItems));

        ListView list = getListView();
        list.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                onListItemClick(v,pos,id);
            }

            protected void onListItemClick(View v, int pos, long id) {
                Intent intent;
                switch((int)id){
                    case 0:
                        intent = new Intent(SettingsActivity.this, DropboxBackupActivity.class);
                        intent.putExtra("action", 0);
                        startActivity(intent); 
                        break;
                    case 1:
                        intent = new Intent(SettingsActivity.this, DropboxBackupActivity.class);
                        intent.putExtra("action", 1);
                        startActivity(intent); 
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }

        });
    }

}
