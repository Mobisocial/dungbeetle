package edu.stanford.mobisocial.dungbeetle;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;
import edu.stanford.mobisocial.dungbeetle.util.RichActivity;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;

public class ProfileActivity extends RichActivity{

    private Handler handler = new Handler();
    private boolean mEnablePresenceUpdates = false;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private final String TAG = "ProfileActivity";

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

    protected void editMyProfile(){
        setContentView(R.layout.edit_profile);
        setTitleFromActivityLabel (R.id.title_text);
        final EditText profileName = (EditText) findViewById(R.id.edit_profile_name);
        final EditText profileAbout = (EditText) findViewById(R.id.edit_profile_about);
        profileName.setText(mIdent.userName());

        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
            null, 
            DbObject.TYPE + "=? AND " + DbObject.CONTACT_ID + "=?", new String[]{ "profile" , Long.toString(Contact.MY_ID)}, 
            DbObject.TIMESTAMP + " DESC");

        if(c.moveToFirst()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String name = obj.optString("name");
                String about = obj.optString("about");
                profileName.setText(name);
                profileAbout.setText(about);              
            }catch(JSONException e){}
        }
        else {
            profileName.setText(mIdent.userName());
        }

        Button saveButton = (Button) findViewById(R.id.save_profile_button);
        saveButton.setOnClickListener(new OnClickListener(){
                public void onClick(View v)
                {
                    String name = profileName.getText().toString();
                    String about = profileAbout.getText().toString();
                    mHelper.setMyName(profileName.getText().toString());
                    Helpers.updateProfile(ProfileActivity.this, name, about);
                    
                    //updateProfileToGroups();
                    finish();
                }
            });

    }




    protected void viewMyProfile(){
        setContentView(R.layout.view_self_profile);
        setTitleFromActivityLabel (R.id.title_text);
        final TextView profileName = (TextView) findViewById(R.id.view_profile_name);
        final TextView profileEmail = (TextView) findViewById(R.id.view_profile_email);
        final TextView profileAbout = (TextView) findViewById(R.id.view_profile_about);
        final ImageView icon = (ImageView) findViewById(R.id.icon);

        Button edit_profile_button = (Button)this.findViewById(R.id.edit_profile_button);
        edit_profile_button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                    intent.putExtra("edit", 1);
                    startActivity(intent); 
                }
            });
        
        Spinner presence = (Spinner)this.findViewById(R.id.presence);

        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
            null, 
            DbObject.TYPE + "=? AND " + DbObject.CONTACT_ID + "=?", new String[]{ "profile" , Long.toString(Contact.MY_ID)}, 
            DbObject.TIMESTAMP + " DESC");

        if(c.moveToFirst()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String name = obj.optString("name");
                String about = obj.optString("about");
                profileName.setText(name);
                profileEmail.setText(mIdent.userEmail());
                profileAbout.setText(about);                     
            }catch(JSONException e){}
        }
        else {
            profileName.setText(mIdent.userName());
            profileEmail.setText(mIdent.userEmail());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            Presence.presences);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presence.setAdapter(adapter);

        c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
            null, 
            DbObject.TYPE + "=?", 
            new String[]{ PresenceObj.TYPE}, 
            DbObject.TIMESTAMP + " DESC");

        if(c.moveToFirst()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                int myPresence = Integer.parseInt(obj.optString("presence"));
                mEnablePresenceUpdates = false;
                presence.setSelection(myPresence);
            }catch(JSONException e){}
        }

        c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
            null, 
            DbObject.TYPE + "=?", 
            new String[]{ ProfilePictureObj.TYPE }, 
            DbObject.TIMESTAMP + " DESC");

        if(c.moveToFirst()) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));

            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String bytes = obj.optString(ProfilePictureObj.DATA);
                Log.w(TAG, "picture size: " + bytes.length());
                ((App)getApplication()).objectImages.lazyLoadImage(bytes.hashCode(), bytes, icon);
            }catch(JSONException e){}
        }
        presence.setOnItemSelectedListener(new PresenceOnItemSelectedListener());

        icon.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(ProfileActivity.this,
                                   "Loading camera...", 
                                   Toast.LENGTH_SHORT).show();
                    doActivityForResult(
                        ProfileActivity.this, 
                        new PhotoTaker(
                            ProfileActivity.this, 
                            new PhotoTaker.ResultHandler() {
                                @Override
                                public void onResult(byte[] data) {
                                    Helpers.updatePicture(ProfileActivity.this, data);
                                    //updateProfileToGroups();
                                }
                            }, 200, false));
                }
            });

    }



    private void updateProfileToGroups() {
        try{
            Cursor grps = mHelper.queryDynamicGroups();
            grps.moveToFirst();
            while(!grps.isAfterLast()){
                handleUpdate(new Group(grps), true);
                grps.moveToNext();
            }
            
            grps.close();
        }
        catch(Exception e){
        }
    }
    private void handleUpdate(final Group g, final boolean updateProfile){
        final Uri uri = Uri.parse(g.dynUpdateUri);
        final GroupProviders.GroupProvider h = GroupProviders.forUri(uri);
        new Thread(){
            public void run(){
                h.handle(g.id, uri, ProfileActivity.this, mIdent, g.version, updateProfile);
            }
        }.start();
    }


    protected void viewProfile(long contactId){
        setContentView(R.layout.view_profile);
        TextView profileName = (TextView) findViewById(R.id.view_profile_name);
        TextView profileEmail = (TextView) findViewById(R.id.view_profile_email);
        TextView profileAbout = (TextView) findViewById(R.id.view_profile_about);
        Spinner presence = (Spinner)this.findViewById(R.id.presence);
        try{
            Contact contact = mHelper.contactForContactId(contactId).get();
            presence.setVisibility(View.GONE);    

            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
                null, 
                DbObject.TYPE + "=? AND " + DbObject.CONTACT_ID + "=?", 
                new String[]{ "profile" , Long.toString(contact.id)}, 
                DbObject.TIMESTAMP + " DESC");

            if(c.moveToFirst()) {
                String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    profileName.setText(obj.optString("name"));
                    profileAbout.setText(obj.optString("about"));
                    profileEmail.setText(contact.email);
                }catch(JSONException e){}
            }
            else {
                profileName.setText(contact.name);
                profileEmail.setText(contact.email);
            }  
	    
            final ImageView icon = (ImageView) findViewById(R.id.icon);
            ((App)getApplication()).contactImages.lazyLoadContactPortrait(contact, icon, 200);
            //((App)getApplication()).objectImages.lazyLoadImage(contact.picture.hashCode(), contact.picture, icon);

        }
        catch(Maybe.NoValError e){}
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new DBHelper(ProfileActivity.this);
        mIdent = new DBIdentityProvider(mHelper);


        refresh();

        // Listen for future changes
        ProfileContentObserver profileContentObserver = new ProfileContentObserver(handler);
        getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"), 
            true, profileContentObserver);

    }


    protected void refresh(){
        Intent intent = getIntent();
        long contact_id = intent.getLongExtra("contact_id", -1);
        if(!intent.hasExtra("edit") && contact_id == Contact.MY_ID) {
            viewMyProfile();
        }
        else if(!intent.hasExtra("edit") && contact_id != Contact.MY_ID){
            viewProfile(contact_id);
        }
        else if(intent.hasExtra("edit")){
            editMyProfile();
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
    private final static int ANON = 1;

    /*public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, EDIT, 0, "Edit Profile");
        //menu.add(1, ANON, 1, "Add anon profile");
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
    }*/

    private Uri mImageCaptureUri;
    
    private static final int TAKE_PHOTO_CODE = 1;
    

    private File getTempFile(Context context){
        //it will return /sdcard/image.tmp
        final File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );
        if(!path.exists()){
            path.mkdir();
        }
        return new File(path, "image.tmp");
    }


    @Override
    public void finish() {
        super.finish();
    }

    private class PresenceOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // fix bug where initial selection firing event
            if(mEnablePresenceUpdates){ 
                Helpers.updatePresence(ProfileActivity.this, pos);
            }
            else{
                mEnablePresenceUpdates = true;
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    private class ProfileContentObserver extends ContentObserver {

        public ProfileContentObserver(Handler h) {
            super(h);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            refresh();
        }

    }
}
