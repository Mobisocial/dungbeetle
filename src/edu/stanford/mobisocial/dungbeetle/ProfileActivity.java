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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.content.Context;
import java.io.File;
import java.lang.Math;
import android.provider.MediaStore;
import android.os.Environment;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images.Media;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.BitmapFactory;
import android.util.Base64;


import java.io.ByteArrayOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.os.Handler;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;

public class ProfileActivity extends Activity{

    private Handler handler = new Handler();
    private boolean mEnablePresenceUpdates = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DBHelper helper = new DBHelper(ProfileActivity.this);
        final IdentityProvider ident = new DBIdentityProvider(helper);        
	    final BitmapManager mBitmaps = new BitmapManager(1);

        Intent intent = getIntent();
        Cursor c;
        
        if(!intent.hasExtra("edit")) {
            setContentView(R.layout.view_profile);
            TextView profile_name = (TextView) findViewById(R.id.view_profile_name);
            TextView profile_email = (TextView) findViewById(R.id.view_profile_email);
            TextView profile_about = (TextView) findViewById(R.id.view_profile_about);

            Spinner presence = (Spinner)this.findViewById(R.id.presence);
            
            String email = "";
            String name = "";
            String about = "";
            byte picture[] = null;
        
            long contact_id = intent.getLongExtra("contact_id", -1);

            if(contact_id == Contact.MY_ID) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    Presence.presences);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                presence.setAdapter(adapter);

                c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
                    null, 
                    Object.TYPE + "=?", 
                    new String[]{ PresenceObj.TYPE}, 
                    Object.TIMESTAMP + " DESC");

                if(c.moveToFirst()) {
                    String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                    try{
                        JSONObject obj = new JSONObject(jsonSrc);
                        int myPresence = Integer.parseInt(obj.optString("presence"));
                        presence.setSelection(myPresence);
                        mEnablePresenceUpdates = true;
                    }catch(JSONException e){}
                }

                c = getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"),
                    null, 
                    Object.TYPE + "=?", 
                    new String[]{ ProfilePictureObj.TYPE}, 
                    Object.TIMESTAMP + " DESC");

                if(c.moveToFirst()) {
                    String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                    try{
                        JSONObject obj = new JSONObject(jsonSrc);
                        picture = Base64.decode(obj.optString(ProfilePictureObj.DATA), Base64.DEFAULT);
                        
                    }catch(JSONException e){}
                }

                
                presence.setOnItemSelectedListener(new PresenceOnItemSelectedListener());
            }
            else {
                presence.setVisibility(View.GONE);
            }


            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
                null, 
                Object.TYPE + "=? AND " + Object.CONTACT_ID + "=?", 
                new String[]{ "profile" , Long.toString(contact_id)}, 
                Object.TIMESTAMP + " DESC");

            ProfileContentObserver profileContentObserver = new ProfileContentObserver(handler);
            profileContentObserver.setContactId(contact_id);                

            getContentResolver().registerContentObserver(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"), 
                true, 
                profileContentObserver);

            Contact contact = getContact(contact_id);
            
            if(c.moveToFirst()) {

                String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    name = obj.optString("name");
                    about = obj.optString("about"); 

                    if(contact_id == Contact.MY_ID) {
                        email = ident.userEmail();
                    }
                    else {
                        email = contact.email;
                        picture = contact.picture;
                    }

                }catch(JSONException e){}
            }
            else {
                if(contact_id == Contact.MY_ID) {
                    name = ident.userName();
                    email = ident.userEmail();
                }
                else {
                    name = contact.name;
                    email = contact.email;
                    picture = contact.picture;
                }
            }  
	    

            profile_name.setText(name);
            profile_email.setText(email);
            profile_about.setText(about);

            final ImageView icon = (ImageView) findViewById(R.id.icon);
            if(picture != null) {
                icon.setImageBitmap(BitmapFactory.decodeByteArray(picture, 0, picture.length));
            }
            else{
                icon.setImageResource(R.drawable.anonymous);
            }

            if(contact_id == Contact.MY_ID){
                icon.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Toast.makeText(ProfileActivity.this,
                                           "Loading camera...", 
                                           Toast.LENGTH_SHORT).show();
                            takePhoto();
                        }
                    });
            }
		}

		else {
            setContentView(R.layout.edit_profile);
            final EditText edit_profile_name = (EditText) findViewById(R.id.edit_profile_name);
            final EditText edit_profile_about = (EditText) findViewById(R.id.edit_profile_about);
            edit_profile_name.setText(ident.userName());

            c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
                null, 
                Object.TYPE + "=? AND " + Object.CONTACT_ID + "=?", new String[]{ "profile" , Long.toString(Contact.MY_ID)}, 
                Object.TIMESTAMP + " DESC");

            if(c.moveToFirst()) {

                String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    String name = obj.optString("name");
                    String about = obj.optString("about");
                    
                    edit_profile_name.setText(name);
                    edit_profile_about.setText(about);                     

                }catch(JSONException e){}
            }

            else {
                edit_profile_name.setText(ident.userName());
            }

            
            
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
        }
        helper.close();
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
    private final static int PICTURE = 1;

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

    private Uri mImageCaptureUri;
    
    private static final int TAKE_PHOTO_CODE = 1;
    
    private void takePhoto(){
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(this)) ); 
        
        startActivityForResult(intent, TAKE_PHOTO_CODE);
    }



    private File getTempFile(Context context){
        //it will return /sdcard/image.tmp
        final File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );
        if(!path.exists()){
            path.mkdir();
        }
        return new File(path, "image.tmp");
    }



    @Override

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch(requestCode){
            case TAKE_PHOTO_CODE:
                final File file = getTempFile(this);
                try {
                    BitmapFactory.Options options=new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    Bitmap sourceBitmap=BitmapFactory.decodeFile(file.getPath(),options);

                    
                    //Bitmap sourceBitmap = Media.getBitmap(getContentResolver(), Uri.fromFile(file) );
                    int width = sourceBitmap.getWidth();
                    int height = sourceBitmap.getHeight();
                    int cropSize = Math.min(width, height);
                    Bitmap cropped = Bitmap.createBitmap(sourceBitmap, 0, 0, cropSize, cropSize);

                    int targetSize = 80;
                    float scaleSize = ((float) targetSize) / cropSize;
                    Matrix matrix = new Matrix();
                    // resize the bit map
                    matrix.postScale(scaleSize, scaleSize);
                    matrix.postRotate(270);

                    // recreate the new Bitmap
                    Bitmap resizedBitmap = Bitmap.createBitmap(cropped, 0, 0, 
                                                               cropSize, cropSize, matrix, true);
                    
                    final ImageView icon = (ImageView) findViewById(R.id.icon);
                    icon.setImageBitmap(resizedBitmap);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object   
                    byte[] b = baos.toByteArray(); 

                    Helpers.updatePicture(ProfileActivity.this, b);
                    
                    // do whatever you want with the bitmap (Resize, Rename, Add To Gallery, etc)
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
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


    private class PresenceOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if(mEnablePresenceUpdates){ // fix bug where initial selection firing event
                Helpers.updatePresence(ProfileActivity.this, pos);
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    private class ProfileContentObserver extends ContentObserver {

        long contact_id;

        public ProfileContentObserver(Handler h) {
            super(h);
            contact_id = 0;
        }

        void setContactId(long id) {
            contact_id = id;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
                null, 
                Object.TYPE + "=? AND " + Object.CONTACT_ID + "=?", new String[]{ "profile" , Long.toString(contact_id)}, 
                Object.TIMESTAMP + " DESC");

            if(c.moveToFirst()) {

                String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));

                try{
                    JSONObject obj = new JSONObject(jsonSrc);
                    String name = obj.optString("name");
                    String about = obj.optString("about");  

                    TextView profile_name = (TextView) findViewById(R.id.view_profile_name);
                    TextView profile_about = (TextView) findViewById(R.id.view_profile_about);

                    profile_name.setText(name);
                    profile_about.setText(about);                     

                }catch(JSONException e){}
            }
        }

    }
}
