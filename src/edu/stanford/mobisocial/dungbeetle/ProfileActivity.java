package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.net.Uri;
import android.database.Cursor;
import android.database.ContentObserver;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;

public class ProfileActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
	    final BitmapManager mBitmaps = new BitmapManager(10);

        Intent intent = getIntent();
        Cursor c;
        
        if(!intent.hasExtra("edit")) {

            String email = "email";
            String name = "name";
            String about = "about";
        
            long contact_id = intent.getLongExtra("contact_id", -1);

            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
                null, 
                Object.TYPE + "=? AND " + Object.CONTACT_ID + "=?", new String[]{ "profile" , Long.toString(contact_id)}, 
                Object.TIMESTAMP + " DESC");
            c.registerContentObserver(new ProfileContentObserver());

            if(c.moveToFirst()) {

                String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    name = obj.optString("name");
                    about = obj.optString("about");
                    email = obj.optString("email");                        

                }catch(JSONException e){}
            }
            else {
                if(contact_id == Contact.MY_ID) {
                    DBHelper helper = new DBHelper(ProfileActivity.this);
                    IdentityProvider ident = new DBIdentityProvider(helper);
                    
                    name = ident.userName();
                    email = ident.userEmail();
                    about = "about";
                }
                else {
                    Contact contact = getContact(contact_id);
                    name = contact.name;
                    email = contact.email;
                    about = "about";
                }
            }  
	    

		    setContentView(R.layout.view_profile);
            TextView profile_name = (TextView) findViewById(R.id.view_profile_name);
            TextView profile_email = (TextView) findViewById(R.id.view_profile_email);
            TextView profile_about = (TextView) findViewById(R.id.view_profile_about);

            profile_name.setText(name);
            profile_email.setText(email);
            profile_about.setText(about);

            final ImageView icon = (ImageView) findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mBitmaps.lazyLoadImage(icon, Gravatar.gravatarUri(email));
		}

		else {
                
        
            setContentView(R.layout.edit_profile);
		    final DBHelper helper = new DBHelper(ProfileActivity.this);
            final IdentityProvider ident = new DBIdentityProvider(helper);
            final EditText edit_profile_name = (EditText) findViewById(R.id.edit_profile_name);
            final EditText edit_profile_about = (EditText) findViewById(R.id.edit_profile_about);
            edit_profile_name.setText(ident.userName());
            Button save_button = (Button) findViewById(R.id.save_profile_button);
            save_button.setOnClickListener(new OnClickListener(){
                	public void onClick(View v)
                	{
                	    String name = edit_profile_name.getText().toString();
                	    String about = edit_profile_about.getText().toString();
                		helper.setMyName(edit_profile_name.getText().toString());
                		Helpers.updateProfile(ProfileActivity.this, name, about);
                		//((DungBeetleActivity)(getParent().getParent())).shareContactInfo();
                		finish();
                	}
                });
            helper.close();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        Intent intent = getIntent();
        if(intent.getLongExtra("contact_id", -1) == Contact.MY_ID && !intent.hasExtra("edit")) {
            return true;
        }
        else {
            return false;
        }
    }

    private final static int EDIT = 0;

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, EDIT, 0, "Edit Profile");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case EDIT: {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("edit", 1);
                startActivity(intent); 
                return true;
            }
            default: return false;
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    private Contact getContact(Long id){
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"),
            null, Contact._ID + "=?", 
            new String[]{String.valueOf(id)}, null);
        c.moveToFirst();
        if(c.isAfterLast()){
            return null;
        }
        else{
            Contact contact = new Contact(c);
            return contact;
        }
    }

    private class ProfileContentObserver extends ContentObserver {

        public ProfileContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.w("ProfileActivity", "something changed");
        }

    }
}
