package edu.stanford.mobisocial.dungbeetle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FriendsActivity extends ListActivity{
    
    private ProgressDialog m_ProgressDialog = null; 
    private ArrayList<Friend> m_friends = null;
    private FriendAdapter m_adapter;
    private Runnable viewOrders;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed);
        m_friends = new ArrayList<Friend>();
        this.m_adapter = new FriendAdapter(this, R.layout.row, m_friends);
        setListAdapter(this.m_adapter);
        
        viewOrders = new Runnable(){
            @Override
            public void run() {
                getFriends();
            }
        };
        Thread thread =  new Thread(null, viewOrders, "MagentoBackground");
        thread.start();
        m_ProgressDialog = ProgressDialog.show(FriendsActivity.this,    
              "Please wait...", "Retrieving data ...", true);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
    	
        lv.setOnItemClickListener(new OnItemClickListener() {
          
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent viewContactIntent = new Intent(FriendsActivity.this, ViewContactActivity.class);
				viewContactIntent.putExtra("contact_id", ((Friend)m_friends.get(position)).getId());
				startActivity(viewContactIntent);
				
				
			}
        });
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      //if (v.getId()==R.id.list) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle(((Friend)m_friends.get(info.position)).getUserName());
        //String[] menuItems = getResources().getStringArray(R.array.menu);
        //for (int i = 0; i<menuItems.length; i++) {
          menu.add(Menu.NONE, 0, 0, "Manage groups");
        //}
     // }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
		  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		  //int menuItemIndex = item.getItemId();
		  //String[] menuItems = getResources().getStringArray(R.array.menu);
		  //String menuItemName = menuItems[menuItemIndex];
		  //String listItemName = Countries[info.position];
		  //Intent viewContactIntent = new Intent(FriendsActivity.this, ViewContactActivity.class);
		  //viewContactIntent.putExtra("contact_id", ((Friend)m_friends.get(info.position)).getId());
		  //startActivity(viewContactIntent);
		  //TextView text = (TextView)findViewById(R.id.footer);
		  //text.setText(String.format("Selected %s for item %s", menuItemName, listItemName));
		  Cursor c = getContentResolver().query(
                  Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/groups"), 
                  new String[]{"_id", "group_id", "feed_name"}, 
                  null, null, null);
		  CharSequence[] groups = new CharSequence[c.getCount()];
          Log.i("DBHelper", c.getCount() + " groups");
          if(c.moveToFirst())
          {
              int group_id_col = c.getColumnIndex("group_id");
              int i = 0;
              do{
            	 groups[i] = c.getString(group_id_col);
            	 i++; 
              }while(c.moveToNext());
          }
		  final CharSequence[] items = groups;
		  final boolean[] selected = new boolean[items.length];

		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
		  builder.setTitle("Pick Groups");
		  builder.setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
	            public void onClick(DialogInterface dialog, int item, boolean isChecked) {
	            	String checked = " was checked";
	            	if(isChecked) 
	            		checked = " was checked";
	            	else
            			checked = " was unchecked";
	            	Toast.makeText(getApplicationContext(), items[item] + checked, Toast.LENGTH_SHORT).show();
	            }
	        });
		  builder.setPositiveButton("Done",
                  new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
        	  		
              }
          });
		  AlertDialog alert = builder.create();
		  alert.show();
		  return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.groups_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.new_group:
	        	Intent addGroupIntent = new Intent(FriendsActivity.this, AddGroupActivity.class);
				//should be passing the group id instead, which pulls from contentprovider
		        
				startActivity(addGroupIntent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
    private Runnable returnRes = new Runnable() {

        @Override
        public void run() {
            if(m_friends != null && m_friends.size() > 0){
                m_adapter.notifyDataSetChanged();
                for(int i=0;i<m_friends.size();i++)
                m_adapter.add(m_friends.get(i));
            }
            m_ProgressDialog.dismiss();
            m_adapter.notifyDataSetChanged();
        }
    };
    private void getFriends(){
          try{
              m_friends = new ArrayList<Friend>();
              
              Cursor people = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
              final ArrayList<Contact> contacts = new ArrayList<Contact>();
              while(people.moveToNext()) {
                 int nameFieldColumnIndex = people.getColumnIndex(PhoneLookup.DISPLAY_NAME);
                 String contact = people.getString(nameFieldColumnIndex);
                 int hasNumberFieldColumnIndex = people.getColumnIndex(PhoneLookup.HAS_PHONE_NUMBER);
                 String hasNumber = people.getString(hasNumberFieldColumnIndex);
                 String contactId = people.getString(people.getColumnIndex(ContactsContract.Contacts._ID)); 
                 String photoId = people.getString(people.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                 if(hasNumber.equalsIgnoreCase("1"))
                 {
                	 m_friends.add(new Friend(contact, contactId, photoId));
//              	   contacts.add(new Contact(contactId, contact));
                 }
                 //contactString += contact + ":" + number + "\n";
              }

              people.close();
              Collections.sort(contacts);
              String[] contacts_const = new String[contacts.size()];
              
              for(int i = 0; i < contacts.size(); i++)
              {
              	contacts_const[i] = ((Contact)contacts.get(i)).name;
              }
              
              Log.i("ARRAY", ""+ m_friends.size());
            } catch (Exception e) { 
              Log.e("BACKGROUND_PROC", e.getMessage());
            }
            runOnUiThread(returnRes);
        }
    private class FriendAdapter extends ArrayAdapter<Friend> {

        private ArrayList<Friend> items;

        public FriendAdapter(Context context, int textViewResourceId, ArrayList<Friend> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.row, null);
                }
                Friend o = items.get(position);
                
                    ImageView imageView = (ImageView) v.findViewById(R.id.icon);
                 
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            Long.parseLong(o.getId()));
                    ContentResolver cr = getContentResolver();
                    InputStream input =
                            ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
                    Bitmap contactPhoto = BitmapFactory.decodeStream(input);

                    imageView.setImageBitmap(contactPhoto);
                
                
                
                if (o != null) {
                        TextView tt = (TextView) v.findViewById(R.id.toptext);
                        TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText(o.getUserName()); 
                        }
                        if(bt != null){
                              bt.setText("Id: " + o.getId());
                        }
                }
                return v;
        }
}
}