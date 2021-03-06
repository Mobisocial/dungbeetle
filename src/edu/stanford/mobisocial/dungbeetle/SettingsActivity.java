/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.ui.ColorPickerDialog;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

public class SettingsActivity extends Activity {
    public static final String PREFS_NAME = "DungBeetlePrefsFile";
	private static NearbyActivity.MulticastBroadcastTask mMulticastBroadcast;
	private static final int MULTICAST_DELAY = 2500;
	private static final int MULTICAST_RETRY = 15000;

	private final class VacuumDatabaseListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			new VacuumDatabase().execute();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    //TODO handle here. 
	    if (resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

            SharedPreferences.Editor editor = settings.edit();
            if (uri == null) {
            	editor.putString("ringtone", "none");
            }
            else {
            editor.putString("ringtone", uri.toString());
            }
            editor.commit();
            Log.w("settings", uri.toString());
             
}
	}
	
	private final class SetRingtoneListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			String uri = null;
            Intent intent = new Intent( RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE,
            RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            uri = settings.getString("ringtone", "none");
            
            if(!uri.equals("none"))
            {
                 intent.putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            		 Uri.parse( uri));
            }

            else
            {
                 intent.putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                		 (Uri)null);
            }
        	startActivityForResult(intent, 999);
		}
	}

	private final class SDCardRestoreListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			new AlertDialog.Builder(SettingsActivity.this)
					.setTitle("Restore from SD card?")
					.setMessage("You will lose any unsaved data.")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									new SDCardRestore().execute();
								}
							})
					.setNegativeButton("No", new CancelledDialogListener())
					.show();
		}
	}

	private final class SDCardBackupListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			new AlertDialog.Builder(SettingsActivity.this)
					.setTitle("Backup to SD card?")
					.setMessage("This will overwrite your existing save.")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									new SDCardBackup().execute();
								}
							})
					.setNegativeButton("No", new CancelledDialogListener())
					.show();
		}
	}

	private final class DropboxRestoreListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(SettingsActivity.this,
					DropboxBackupActivity.class);
			intent.putExtra("action", 1);
			startActivity(intent);
		}
	}

	private final class DropboxBackupListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(SettingsActivity.this,
					DropboxBackupActivity.class);
			intent.putExtra("action", 0);
			startActivity(intent);
		}
	}

	private final class DeveloperModeListener implements OnLongClickListener {
		@Override
		public boolean onLongClick(View v) {
			boolean developer_mode = MusubiBaseActivity.getInstance()
					.isDeveloperModeEnabled();
			toast(developer_mode ? "Disabling developer mode."
					: "Enabling developer mode.");
			MusubiBaseActivity.getInstance().setDeveloperMode(!developer_mode);
			// update the dialog by hiding stuff, etc
			loadValues();
			return true;
		}
	}

	private final class CancelledDialogListener implements
			DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {

		}
	}

	private final class GlobalTVModeListener implements OnClickListener {
		public void onClick(View v) {
			boolean global_tv_mode = globalTVMode_.isChecked();
			global_tv_mode = !global_tv_mode;
			getSharedPreferences("main", 0).edit()
					.putBoolean("autoplay", global_tv_mode).commit();
			globalTVMode_.setChecked(global_tv_mode);

			if (global_tv_mode) {
			    // TODO: put in a service.
			    mMulticastBroadcast = new NearbyActivity.MulticastBroadcastTask(
		                SettingsActivity.this, MULTICAST_DELAY, MULTICAST_RETRY);
			    mMulticastBroadcast.execute();

			    try {
    			    JSONObject json = new JSONObject();
    			    json.put(Contact.ATTR_DEVICE_MODALITY, "tv");
    			    Obj imATV = new MemObj("profileupdate", json);
    			    Helpers.sendToEveryone(SettingsActivity.this, imATV);
			    } catch (Exception e) {
			        Log.e(TAG, "Error notifying profile update", e);
			    }
			} else {
			    if (mMulticastBroadcast != null) {
			        mMulticastBroadcast.cancel(true);
			        mMulticastBroadcast = null;
			    }

			    try {
                    JSONObject json = new JSONObject();
                    json.put(Contact.ATTR_DEVICE_MODALITY, "phone");
                    Obj imATV = new MemObj("profileupdate", json);
                    Helpers.sendToEveryone(SettingsActivity.this, imATV);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying profile update", e);
                }
			}
		}
	}

	private final class SecondaryColorListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			final float[] baseHues = Feed.getBaseHues();
			int c = Color.HSVToColor(new float[] { baseHues[1], 1f, 1f });
			ColorPickerDialog cpd = new ColorPickerDialog(
					SettingsActivity.this,
					new ColorPickerDialog.OnColorChangedListener() {
						@Override
						public void colorChanged(int color) {
							float[] hsv = new float[3];
							Color.colorToHSV(color, hsv);
							baseHues[1] = hsv[0];
							Feed.setBaseHues(baseHues);
							SharedPreferences settings = getSharedPreferences(
									HomeActivity.PREFS_NAME, 0);
							settings.edit()
									.putString(
											"baseHues",
											Arrays.toString(baseHues)
													.replaceAll("\\[|\\]", ""))
									.commit();
							//reload colors
							loadValues();
						}
					}, c);
			cpd.show();
		}
	}

	private final class PrimaryColorListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			final float[] baseHues = Feed.getBaseHues();
			int c = Color.HSVToColor(new float[] { baseHues[0], 1f, 1f });
			ColorPickerDialog cpd = new ColorPickerDialog(
					SettingsActivity.this,
					new ColorPickerDialog.OnColorChangedListener() {
						@Override
						public void colorChanged(int color) {
							float[] hsv = new float[3];
							Color.colorToHSV(color, hsv);
							baseHues[0] = hsv[0];
							Feed.setBaseHues(baseHues);
							SharedPreferences settings = getSharedPreferences(
									HomeActivity.PREFS_NAME, 0);
							settings.edit()
									.putString(
											"baseHues",
											Arrays.toString(baseHues)
													.replaceAll("\\[|\\]", ""))
									.commit();
							//reload colors
							loadValues();
						}
					}, c);
			cpd.show();
		}
	}

	String TAG = "Settings";

	Button primaryColor_;
	Button secondaryColor_;
	Button info_;
	TextView vacuumDatabase_;
	TextView setRingtone_;
	
	CheckedTextView globalTVMode_;

	/*** Dashboard stuff ***/
	public void goHome(Context context) {
		final Intent intent = new Intent(context, HomeActivity.class);
        if(Build.VERSION.SDK_INT < 11)
        	intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	else 
    		intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public void setTitleFromActivityLabel(int textViewId) {
		TextView tv = (TextView) findViewById(textViewId);
		if (tv != null)
			tv.setText(getTitle());
	}

	public void onClickHome(View v) {
		goHome(this);
	}

	public void onClickAbout(View v) {
		startActivity(new Intent(getApplicationContext(), AboutActivity.class));
	}

	/*** End Dashboard Stuff ***/

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		setTitleFromActivityLabel(R.id.title_text);

		// set long click on the info mode to toggle developer mode using a
		// toast as feedback
		findViewById(R.id.btn_info).setOnLongClickListener(
				new DeveloperModeListener());

		// save references to the UI elements that show visible state
		primaryColor_ = (Button) findViewById(R.id.primary_color);
		secondaryColor_ = (Button) findViewById(R.id.secondary_color);
		globalTVMode_ = (CheckedTextView) findViewById(R.id.global_tv_mode);
		vacuumDatabase_ = (TextView) findViewById(R.id.vacuum_database);
		setRingtone_ = (TextView) findViewById(R.id.set_ringtone);

		// connect the global tv mode toggle to the shared preferences
		globalTVMode_.setOnClickListener(new GlobalTVModeListener());
		vacuumDatabase_.setOnClickListener(new VacuumDatabaseListener());
		setRingtone_.setOnClickListener(new SetRingtoneListener());
		
		// hook up the color picker dialogs to the buttons
		primaryColor_.setOnClickListener(new PrimaryColorListener());
		secondaryColor_.setOnClickListener(new SecondaryColorListener());

		// call out to the dropboxx activity for network backup and restore
		findViewById(R.id.dropbox_backup).setOnClickListener(
				new DropboxBackupListener());
		findViewById(R.id.dropbox_restore).setOnClickListener(
				new DropboxRestoreListener());

		// connect the local handlers that manage the sd card backup
		findViewById(R.id.sdcard_backup).setOnClickListener(
				new SDCardBackupListener());
		findViewById(R.id.sdcard_restore).setOnClickListener(
				new SDCardRestoreListener());

		loadValues();

	}

	private final class IgnoreSearchKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			//don't let search cancel the operation
			if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
		        return true; // Pretend we processed it
		    }
			return false;
		}
	}

	class VacuumDatabase extends AsyncTask<Void, Void, Void> {
		ProgressDialog progress_;

		@Override
		protected void onPreExecute() {
			progress_ = new ProgressDialog(SettingsActivity.this);
			progress_.setCancelable(false);
			progress_.setMessage("Vacuuming Database...");
			progress_.show();
			int orientation = getResources().getConfiguration().orientation;
			SettingsActivity.this.setRequestedOrientation(orientation);
		}
		@Override
		protected Void doInBackground(Void... params) {
			try {
				DBHelper mHelper = DBHelper.getGlobal(SettingsActivity.this);
				mHelper.vacuum();
			} catch (Exception e) {
				Log.e(TAG, "Failure doing chores (vacuuming)", e);
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			SettingsActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			progress_.dismiss();
		}
	}
	
	class SDCardBackup extends AsyncTask<Void, Integer, Exception> {
		ProgressDialog progress_;

		@Override
		protected void onPreExecute() {
			progress_ = new ProgressDialog(SettingsActivity.this);
			progress_.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress_.setOnKeyListener(new IgnoreSearchKeyListener());
			progress_.setCancelable(false);
			progress_.setMessage("Backing up to SD card.");
			progress_.setMax(100);
			progress_.setIndeterminate(false);
			progress_.show();
			int orientation = getResources().getConfiguration().orientation;
			SettingsActivity.this.setRequestedOrientation(orientation);
		}
		@Override
		protected void onProgressUpdate(Integer... values) {
			progress_.setProgress(values[0].intValue());
		}

		@Override
		protected Exception doInBackground(Void... params) {
			try {
				DBHelper mHelper = DBHelper.getGlobal(SettingsActivity.this);
				mHelper.getReadableDatabase().close();
				File data = Environment.getDataDirectory();
				String currentDBPath = "/data/edu.stanford.mobisocial.dungbeetle/databases/"
						+ DBHelper.DB_NAME;
				String extStorageDirectory = Environment
						.getExternalStorageDirectory().toString()
						+ "/MusubiBackup/";

				File backupDB = new File(extStorageDirectory, DBHelper.DB_NAME);
				File fileDirectory = new File(extStorageDirectory);
				fileDirectory.mkdirs();

				File currentDB = new File(data, currentDBPath);
				long file_size = currentDB.length();
				InputStream in = new FileInputStream(currentDB);
				OutputStream out = new FileOutputStream(backupDB);
				byte[] buf = new byte[65536];
				int len;
				long so_far = 0;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
					so_far += len;
					publishProgress((int) (100 * so_far / (file_size + 1)));
				}
				in.close();
				out.close();

				return null;
			} catch (Exception e) {
				Log.e(TAG, "Failure backing up to SD card", e);
				return e;
			}
		}

		@Override
		protected void onPostExecute(Exception result) {
			SettingsActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			progress_.dismiss();
			if (result == null) {
				toast("Backup complete!");
			} else {
				toast("Backup failed: " + result.getMessage());
			}

		}
	}

	class SDCardRestore extends AsyncTask<Void, Integer, Exception> {

		ProgressDialog progress_;
		DBHelper helper_;

		@Override
		protected void onPreExecute() {
			helper_ = DBHelper.getGlobal(SettingsActivity.this);
			progress_ = new ProgressDialog(SettingsActivity.this);
			progress_.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress_.setOnKeyListener(new IgnoreSearchKeyListener());
			progress_.setCancelable(false);
			progress_.setMessage("Restoring from SD card.");
			progress_.setMax(100);
			progress_.setIndeterminate(false);
			progress_.show();
			int orientation = getResources().getConfiguration().orientation;
			SettingsActivity.this.setRequestedOrientation(orientation);
		}
		@Override
		protected void onProgressUpdate(Integer... values) {
			progress_.setProgress(values[0].intValue());
		}

		@Override
		protected Exception doInBackground(Void... params) {
			try {
				helper_.getReadableDatabase().close();
				File data = Environment.getDataDirectory();
				String extStorageDirectory = Environment
						.getExternalStorageDirectory().toString()
						+ "/MusubiBackup/";
				String dbPath = extStorageDirectory + DBHelper.DB_NAME;
				// Close the SQLiteOpenHelper so it will commit the created
				// empty database to internal storage.
				helper_.close();

				data = Environment.getDataDirectory();
				File newDb = new File(dbPath);
				File oldDb = new File(data, helper_.getDatabasePath());
				if (!newDb.exists()) {
					throw new RuntimeException("Backup database not found");
				}
				InputStream in = new FileInputStream(newDb);
				OutputStream out = new FileOutputStream(oldDb);
				long file_size = newDb.length();
				byte[] buf = new byte[65536];
				int len;
				long so_far = 0;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
					so_far += len;
					publishProgress((int) (100 * so_far / (file_size + 1)));
				}
				in.close();
				out.close();
				// Access the copied database so SQLiteHelper will cache it and
				// mark it as created.
				helper_.getWritableDatabase().close();
				helper_.close();

				//kill because the old toggle code really never worked for me
				Process.killProcess(Process.myPid());
				return null;
			} catch (Exception e) {
				Log.e(TAG, "Failure restoring from SD card", e);
				return e;
			}
		}

		@Override
		protected void onPostExecute(Exception result) {
			SettingsActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			progress_.dismiss();
			if (result == null) {
				//we'll never get here because it will have restarted
				toast("Restore complete!");
			} else {
				toast("Restore failed: " + result.getMessage());
			}

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadValues();
	}

	void loadValues() {
		boolean developer_mode = MusubiBaseActivity.getInstance()
				.isDeveloperModeEnabled();
		globalTVMode_.setVisibility(developer_mode ? View.VISIBLE
				: View.INVISIBLE);
		vacuumDatabase_.setVisibility(developer_mode ? View.VISIBLE
				: View.INVISIBLE);

		final float[] baseHues = Feed.getBaseHues();
		int c0 = Color.HSVToColor(new float[] { baseHues[0], 1f, 1f });
		int c1 = Color.HSVToColor(new float[] { baseHues[1], 1f, 1f });
		primaryColor_.setBackgroundColor(c0);
		secondaryColor_.setBackgroundColor(c1);

		boolean global_tv_mode = getSharedPreferences("main", 0).getBoolean(
				"autoplay", false);
		globalTVMode_.setChecked(global_tv_mode);
	}

	public void toast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		error.show();
	}
}
