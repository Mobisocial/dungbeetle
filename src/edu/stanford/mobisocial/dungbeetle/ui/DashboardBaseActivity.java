/*
 * Copyright (C) 2011 Wglxy.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.stanford.mobisocial.dungbeetle.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.AboutActivity;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.RemoteControlReceiver;
import edu.stanford.mobisocial.dungbeetle.SearchActivity;
import edu.stanford.mobisocial.dungbeetle.model.PresenceAwareNotify;
import edu.stanford.mobisocial.dungbeetle.util.ActivityCallout;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.RemoteControlRegistrar;

/**
 * This is the base class for activities in the dashboard application. It
 * implements methods that are useful to all top level activities. That
 * includes: (1) stub methods for all the activity lifecycle methods; (2)
 * onClick methods for clicks on home, search, feature 1, feature 2, etc. (3) a
 * method for displaying a message to the screen via the Toast class.
 */

public abstract class DashboardBaseActivity extends FragmentActivity implements InstrumentedActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "msb-dashbaord";
    private static int REQUEST_ACTIVITY_CALLOUT = 39;
    private static ActivityCallout mCurrentCallout;
    private static DashboardBaseActivity sInstance;


    /**
     * onCreate - called when the activity is first created. Called when the
     * activity is first created. This is where you should do all of your normal
     * static set up: create views, bind data to lists, etc. This method also
     * provides you with a Bundle containing the activity's previously frozen
     * state, if there was one. Always followed by onStart().
     */

    protected DBHelper mHelper;
    private RemoteControlRegistrar remoteControlRegistrar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        mHelper = new DBHelper(this);
        remoteControlRegistrar = new RemoteControlRegistrar(this, RemoteControlReceiver.class);
    }

    /**
     * onDestroy The final call you receive before your activity is destroyed.
     * This can happen either because the activity is finishing (someone called
     * finish() on it, or because the system is temporarily destroying this
     * instance of the activity to save space. You can distinguish between these
     * two scenarios with the isFinishing() method.
     */

    protected void onDestroy() {
        super.onDestroy();
        remoteControlRegistrar.unregisterRemoteControl();
    }

    /**
     * onPause Called when the system is about to start resuming a previous
     * activity. This is typically used to commit unsaved changes to persistent
     * data, stop animations and other things that may be consuming CPU, etc.
     * Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns. Followed by
     * either onResume() if the activity returns back to the front, or onStop()
     * if it becomes invisible to the user.
     */

    protected void onPause() {
        super.onPause();
        mResumed = false;
    }

    /**
     * onRestart Called after your activity has been stopped, prior to it being
     * started again. Always followed by onStart().
     */

    protected void onRestart() {
        super.onRestart();
    }

    /**
     * onResume Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with
     * user input going to it. Always followed by onPause().
     */

    protected void onResume() {
        super.onResume();
        sInstance = this;
        remoteControlRegistrar.registerRemoteControl();
        new PresenceAwareNotify(this).cancelAll();
        mResumed = true;
    }

    /**
     * onStart Called when the activity is becoming visible to the user.
     * Followed by onResume() if the activity comes to the foreground, or
     * onStop() if it becomes hidden.
     */

    protected void onStart() {
        super.onStart();
    }

    /**
     * onStop Called when the activity is no longer visible to the user because
     * another activity has been resumed and is covering this one. This may
     * happen either because a new activity is being started, an existing one is
     * being brought in front of this one, or this one is being destroyed.
     * Followed by either onRestart() if this activity is coming back to
     * interact with the user, or onDestroy() if this activity is going away.
     */

    protected void onStop() {
        super.onStop();
    }

    /**
 */
    // Click Methods

    /**
     * Handle the click on the home button.
     * 
     * @param v View
     * @return void
     */

    public void onClickHome(View v) {
        goHome(this);
    }

    /**
     * Handle the click on the search button.
     * 
     * @param v View
     * @return void
     */

    public void onClickSearch(View v) {
        startActivity(new Intent(getApplicationContext(), SearchActivity.class));
    }

    /**
     * Handle the click on the About button.
     * 
     * @param v View
     * @return void
     */

    public void onClickAbout(View v) {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
    }

    /**
 */
    // More Methods

    /**
     * Go back to the home activity.
     * 
     * @param context Context
     * @return void
     */

    public void goHome(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }


    /**
     * Send a message to the debug log and display it using Toast.
     */
    public void trace(String msg) {
        Log.d("Demo", msg);
        toast(msg);
    }

    /**
     * Use the activity label to set the text in the activity's title text view.
     * The argument gives the name of the view.
     * <p>
     * This method is needed because we have a custom title bar rather than the
     * default Android title bar. See the theme definitons in styles.xml.
     * 
     * @param title The text to display, or null to use the activity's label.
     * @return void
     */
    public static void doTitleBar(Activity activity, String title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            View titleBar = activity.findViewById(R.id.dashboardTitleBar);
            if (titleBar != null) {
                titleBar.setVisibility(View.GONE);
            }
        } else {
            // Dashboard title bar
            TextView tv = (TextView) activity.findViewById(R.id.title_text);
            if (tv == null) {
                return;
            }
            if (title == null) {
                tv.setText(activity.getTitle());
            } else {
                tv.setText(title);
            }
        }
    }

    public static void doTitleBar(Activity activity) {
        doTitleBar(activity, null);
    }

    public void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DashboardBaseActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void showDialog(Dialog dialog) {
        dialog.show(); // TODO: Figure out how to preserve dialog during screen rotation.
    }

    public void doActivityForResult(ActivityCallout callout) {
        mCurrentCallout = callout;
        Intent launch = callout.getStartIntent();
        startActivityForResult(launch, REQUEST_ACTIVITY_CALLOUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTIVITY_CALLOUT) {
            mCurrentCallout.handleResult(resultCode, data);
        }
    }

    public boolean isDeveloperModeEnabled() {
        return getSharedPreferences("main", 0).getBoolean("dev_mode", false);
    }

    public void setDeveloperMode(boolean enabled) {
        getSharedPreferences("main", 0).edit().putBoolean("dev_mode", enabled).commit();
    }

    public static DashboardBaseActivity getInstance() {
        return sInstance;
    }

    public RemoteControlRegistrar getRemoteControlRegistrar() {
        return remoteControlRegistrar;
    }

    private boolean mResumed;
    public boolean amResumed() {
        return mResumed;
    }

    private Uri mFeedUri;
    public void setFeedUri(Uri feedUri) {
        mFeedUri = feedUri;
    }

    public void clearFeedUri() {
        mFeedUri = null;
    }

    public Uri getFeedUri() {
        return mFeedUri;
    }

    private KeyEvent.Callback mOnKeyListener;
    public void setOnKeyListener(KeyEvent.Callback listener) {
        mOnKeyListener = listener;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mOnKeyListener != null) {
            if (mOnKeyListener.onKeyUp(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (mOnKeyListener != null) {
            if (mOnKeyListener.onKeyLongPress(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mOnKeyListener != null) {
            if (mOnKeyListener.onKeyDown(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if (mOnKeyListener != null) {
            if  (mOnKeyListener.onKeyMultiple(keyCode, repeatCount, event)) {
                return true;
            }
        }
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }
}

