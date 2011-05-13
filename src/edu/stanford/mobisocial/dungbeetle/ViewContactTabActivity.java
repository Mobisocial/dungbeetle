package edu.stanford.mobisocial.dungbeetle;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class ViewContactTabActivity extends TabActivity
{


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create top-level tabs
        //Resources res = getResources();
        // res.getDrawable(R.drawable.icon)

        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;  

        Intent intent = getIntent();
        Long contact_id = intent.getLongExtra("contact_id", -1);
        String contact_name = intent.getStringExtra("contact_name");

        if(intent.hasExtra("group_name")) {
            String group_name = intent.getStringExtra("group_name");
            setTitle("Groups > " + group_name + " > Members > " + contact_name);
        }
        else {
            setTitle("Contacts > " + contact_name);
        }


        intent = new Intent().setClass(this, ProfileActivity.class);
        intent.putExtra("contact_id", contact_id);
        spec = tabHost.newTabSpec("contacts").setIndicator(
            "Profile",
            null).setContent(intent);
        tabHost.addTab(spec);

                
        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ObjectsActivity.class);
        intent.putExtra("contact_id", contact_id);
        spec = tabHost.newTabSpec("objects").setIndicator(
            "Feed",
            null).setContent(intent);
        tabHost.addTab(spec);


		
        tabHost.setCurrentTab(0);

   
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
    }


}




