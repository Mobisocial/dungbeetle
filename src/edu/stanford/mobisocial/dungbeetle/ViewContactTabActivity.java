package edu.stanford.mobisocial.dungbeetle;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.ui.FeedHomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;
import edu.stanford.mobisocial.dungbeetle.ui.MyProfileActivity;

public class ViewContactTabActivity extends TabActivity
{

    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }

    public void setTitleFromActivityLabel (int textViewId, String title)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (title);
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
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        findViewById(R.id.btn_broadcast).setVisibility(View.GONE);
        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  

        Intent intent = getIntent();
        Long contact_id;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            contact_id = Long.parseLong(intent.getData().getLastPathSegment());
        } else {
            contact_id = intent.getLongExtra("contact_id", -1);
        }
        String contact_name = intent.getStringExtra("contact_name");

        if(intent.hasExtra("group_name")) {
            String group_name = intent.getStringExtra("group_name");
            setTitle("Groups > " + group_name + " > Members > " + contact_name);
        }
        else {
            setTitle("Contacts > " + contact_name);
            
        }

        setTitleFromActivityLabel (R.id.title_text, contact_name);

        intent = new Intent().setClass(this, MyProfileActivity.class);
        intent.putExtra("contact_id", contact_id);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Profile",
            null).setContent(intent);
        tabHost.addTab(spec);

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, FeedHomeActivity.class);
        intent.putExtra("contact_id", contact_id);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feed",
            null).setContent(intent);
        //tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
