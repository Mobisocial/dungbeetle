package edu.stanford.mobisocial.dungbeetle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.facebook.FacebookInterfaceActivity;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Gravatar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ContactsActivity extends ListActivity implements OnItemClickListener{
	private ContactListCursorAdapter mContacts;
    public static final String SHARE_SCHEME = "db-share-contact";
	protected final BitmapManager mBitmaps = new BitmapManager(10);
	private NotificationManager mNotificationManager;


    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent intent = getIntent();
        if(intent.hasExtra("group_id")){
            Long group_id = intent.getLongExtra("group_id", -1);
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/group_members/" + group_id),
                new String[]{Contact._ID, 
                             Contact.NAME, 
                             Contact.EMAIL, 
                             Contact.PERSON_ID,
                             Contact.PUBLIC_KEY},
                null, null, null);
            mContacts = new ContactListCursorAdapter(this, c);
        }
        else{
            Cursor c = getContentResolver().query(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), 
                new String[]{Contact._ID, 
                             Contact.NAME, 
                             Contact.EMAIL, 
                             Contact.PERSON_ID,
                             Contact.PUBLIC_KEY}, 
                null, null, null);
            mContacts = new ContactListCursorAdapter(this, c);
        }

		setListAdapter(mContacts);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
		lv.setOnItemClickListener(this);
	}


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){

        Cursor cursor = (Cursor)mContacts.getItem(position);

        final Contact c = new Contact(cursor);
        final CharSequence[] items = new CharSequence[]{ "Send Message", "Start Application", "Manage Groups" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actions");
        builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item){
                    case 0:
                        sendMessageToContact(c);
                        break;
                    case 1:
                        startApplicationWithContact(c);
                        break;
                    case 2:
                    	UIHelpers.showGroupPicker(ContactsActivity.this, c);
                    	break;
                    }
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void sendMessageToContact(final Contact contact){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Enter message:");
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    DBHelper helper = new DBHelper(ContactsActivity.this);
                    Helpers.sendIM(
                        ContactsActivity.this, 
                        Collections.singletonList(contact),
                        input.getText().toString());
                }
            });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
        alert.show();
    }

    private void startApplicationWithContact(final Contact contact){
        final PackageManager mgr = getPackageManager();
        Intent i = new Intent("android.intent.action.CONFIGURE");
        i.addCategory("android.intent.category.P2P");
        final List<ResolveInfo> infos = mgr.queryBroadcastReceivers(i, 0);
        if(infos.size() > 0){
            ArrayList<String> names = new ArrayList<String>();
            for(ResolveInfo info : infos){
                names.add(info.loadLabel(mgr).toString());
            }
            final CharSequence[] items = names.toArray(new CharSequence[]{});
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Share application:");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        final ResolveInfo info = infos.get(item);
                        Intent i = new Intent();
                        i.setClassName(info.activityInfo.packageName, 
                                       info.activityInfo.name);
                        i.setAction("android.intent.action.CONFIGURE");
                        i.addCategory("android.intent.category.P2P");
                        BroadcastReceiver rec = new BroadcastReceiver(){
                                public void onReceive(Context c, Intent i){
                                    Intent launch = new Intent();
                                    launch.setAction(Intent.ACTION_MAIN);
                                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                                    launch.setPackage(info.activityInfo.packageName);
                                    List<ResolveInfo> resolved = 
                                        mgr.queryIntentActivities(launch, 0);
                                    if (resolved.size() > 0) {
                                        ActivityInfo info = resolved.get(0).activityInfo;
                                        String arg = getResultData();
                                        launch.setComponent(new ComponentName(
                                                                info.packageName,
                                                                info.name));
                                        launch.putExtra("creator", true);
                                        launch.putExtra(
                                            "android.intent.extra.APPLICATION_ARGUMENT",
                                            arg);
                                        startActivity(launch);
                                        Helpers.sendApplicationInvite(
                                            ContactsActivity.this,
                                            Collections.singletonList(contact), 
                                            info.packageName, arg);
                                    }
                                    else{
                                        Toast.makeText(getApplicationContext(), 
                                                       "Sorry, no response from applications.",
                                                       Toast.LENGTH_SHORT).show();
                                    }
                                }
                            };
                        sendOrderedBroadcast(i, null, rec, null, RESULT_OK, null, null);
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else{
            Toast.makeText(getApplicationContext(), 
                           "Sorry, couldn't find any compatible apps.", 
                           Toast.LENGTH_SHORT).show();
        }
    }


    private class ContactListCursorAdapter extends CursorAdapter {

        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            bindView(v, context, c);
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndexOrThrow(Contact.NAME));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(name);

            String email = c.getString(c.getColumnIndexOrThrow(Contact.EMAIL));
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mBitmaps.lazyLoadImage(icon, Gravatar.gravatarUri(email));
        }

    }


    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        menu.clear();
        menu.add(0, 0, 0, "Set email (debug)");
        menu.add(0, 1, 0, "Facebook Bootstrap");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case 0: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Enter email:");
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DBHelper helper = new DBHelper(ContactsActivity.this);
                        helper.setMyEmail(input.getText().toString());
                    }
                });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
            alert.show();
            return true;
        }
        case 1: {
            Intent intent = new Intent(this, FacebookInterfaceActivity.class);
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

}



