package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.google.OAuthFlowApp;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.ColorPickerDialog;
import edu.stanford.mobisocial.dungbeetle.ui.DashboardBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;



public class SettingsActivity extends ListActivity {
    String[] listItems = {"Backup to Dropbox", "Restore from Dropbox", "Backup to SD card", "Restore from SD card", "Primary Color", "Secondary Color"};//, "Wipe Data (Keep identity)", "Start from Scratch"};

    String TAG = "Settings";


/*** Dashbaord stuff ***/
    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
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
        setListAdapter(new ArrayAdapter<String>(this, 
        android.R.layout.simple_list_item_1, listItems));

        ListView list = getListView();
        list.setOnItemClickListener(mSettingsClicked);
        findViewById(R.id.btn_info).setOnLongClickListener(mDevModeListener);
    }

    private void restoreFromSd() {
        try{
            DBHelper mHelper = new DBHelper(SettingsActivity.this);
            mHelper.getReadableDatabase().close();
            File data = Environment.getDataDirectory();
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/MusubiBackup/";

            File legacyDB = new File(Environment.getExternalStorageDirectory().toString() + "/DungBeetleBackup/" + DBHelper.OLD_DB_NAME);
            if(legacyDB.exists()) {
                Log.w(TAG, "legacy db exists, backup from here then delete");
                mHelper.importDatabaseFromSD(Environment.getExternalStorageDirectory().toString() + "/DungBeetleBackup/" + DBHelper.OLD_DB_NAME);
                legacyDB.delete();

                String currentDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"+DBHelper.DB_NAME;
                File backupDB = new File(extStorageDirectory, DBHelper.DB_NAME);
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
                
            }
            else {
                mHelper.importDatabaseFromSD(extStorageDirectory+DBHelper.DB_NAME);
            }
            toast("restored");
        }
        catch(Exception e){
            toast("failed to restore");
        }
    }

    public void toast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        error.show();
    }

    private OnItemClickListener mSettingsClicked = new ListView.OnItemClickListener() {
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
                        // TODO: asynchronous writing. Or, at least, off the ui thread.
                        // TODO: really, we'd like continous backups : )
                        DBHelper mHelper = new DBHelper(SettingsActivity.this);
                        mHelper.getReadableDatabase().close();
                        File data = Environment.getDataDirectory();
                        String currentDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"+DBHelper.DB_NAME;
                        String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/MusubiBackup/";

                        File backupDB = new File(extStorageDirectory, DBHelper.DB_NAME);
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

                        File legacyDB = new File(Environment.getExternalStorageDirectory().toString() + "/DungBeetleBackup/" + DBHelper.OLD_DB_NAME);
                        if(legacyDB.exists()) {
                            legacyDB.delete();
                        }
                        toast("Backup complete.");
                    }
                    catch(Exception e){
                        toast("Backup failed.");
                        Log.e(TAG, "Failed to backup to sd card", e);
                    }
                    
                    break;
                case 3:
                    new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Restore from SD card?")
                        .setMessage("You will lose any unsaved data.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                restoreFromSd();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
                    break;
                case 4: {
                    final float[] baseHues = Feed.getBaseHues();
                    int c = Color.HSVToColor(new float[] { baseHues[0], 1f, 1f });
                    ColorPickerDialog cpd = new ColorPickerDialog(SettingsActivity.this, 
                        new ColorPickerDialog.OnColorChangedListener() {
                            @Override
                            public void colorChanged(int color) {
                                float[] hsv = new float[3];
                                Color.colorToHSV(color, hsv);
                                baseHues[0] = hsv[0];
                                Feed.setBaseHues(baseHues);
                                SharedPreferences settings = getSharedPreferences(HomeActivity.PREFS_NAME, 0);
                                settings.edit().putString("baseHues", Arrays.toString(baseHues).replaceAll("\\[|\\]", "")).commit();
                            }
                        }, c);
                    cpd.show();
                    break;
                }
                case 5: {
                    final float[] baseHues = Feed.getBaseHues();
                    int c = Color.HSVToColor(new float[] { baseHues[1], 1f, 1f });
                    ColorPickerDialog cpd = new ColorPickerDialog(SettingsActivity.this, 
                        new ColorPickerDialog.OnColorChangedListener() {
                            @Override
                            public void colorChanged(int color) {
                                float[] hsv = new float[3];
                                Color.colorToHSV(color, hsv);
                                baseHues[1] = hsv[0];
                                Feed.setBaseHues(baseHues);
                                SharedPreferences settings = getSharedPreferences(HomeActivity.PREFS_NAME, 0);
                                settings.edit().putString("baseHues", Arrays.toString(baseHues).replaceAll("\\[|\\]", "")).commit();
                            }
                        }, c);
                    cpd.show();
                    break;
                }
                case 6:
                    break;
                case 7:
                    SharedPreferences p = getSharedPreferences("main", 0);
                    boolean old = p.getBoolean("autoplay", false);
                    if (old) {
                        toast("Exiting Party Mode");
                    } else {
                        toast("Party Mode enabled");
                    }
                    p.edit().putBoolean("autoplay", !old).commit();
                    break;
                case 8:
                    intent = new Intent(SettingsActivity.this, OAuthFlowApp.class);
                    startActivity(intent);
                    break;
            }
        }
    };

    private OnLongClickListener mDevModeListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            boolean wasEnabled = DashboardBaseActivity.getInstance().isDeveloperModeEnabled();
            if (wasEnabled) {
                toast("Disabling developer mode...");
            } else {
                toast("Enabling developer mode...");
            }
            DashboardBaseActivity.getInstance().setDeveloperMode(!wasEnabled);
            return true;
        }
    };
}
