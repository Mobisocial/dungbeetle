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
import java.io.*;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.google.*;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import android.os.Environment;



public class SettingsActivity extends ListActivity {
    String[] listItems = {"Backup to Dropbox", "Restore from Dropbox", "Backup to SD card", "Restore from SD card"};//, "Wipe Data (Keep identity)", "Start from Scratch"};
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
                        try{
                            DBHelper mHelper = new DBHelper(SettingsActivity.this);
                            mHelper.getReadableDatabase().close();
                            File data = Environment.getDataDirectory();
                            String currentDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"+DBHelper.DB_NAME;
                            String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/DungBeetleBackup/";
                            
                            File backupDB = new File(extStorageDirectory, "DUNG_HEAP.db");
                            File fileDirectory = new File(extStorageDirectory);
                            fileDirectory.mkdirs();
                            
                            File currentDB = new File(data, currentDBPath);
                            InputStream in = new FileInputStream(currentDB);
                            OutputStream out = new FileOutputStream(backupDB);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0){
                                out.write(buf, 0, len);
                            }
                            in.close();
                            out.close();
                            showToast("backed up");
                        }
                        catch(Exception e){
                            showToast("failed to back up");
                        }
                        
                        break;
                    case 3:
                        try{
                            DBHelper mHelper = new DBHelper(SettingsActivity.this);
                            mHelper.getReadableDatabase().close();
                            File data = Environment.getDataDirectory();
                            String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/DungBeetleBackup/";
                            
                            
                            mHelper.importDatabaseFromSD(extStorageDirectory+"DUNG_HEAP.db");
                            showToast("restored");
                        }
                        catch(Exception e){
                            showToast("failed to restore");
                        }
                        break;
                    case 4:
                        intent = new Intent(SettingsActivity.this, OAuthFlowApp.class);
                        startActivity(intent);
                        break;
                }
            }

        });
    }



    public void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
