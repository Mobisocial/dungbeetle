package edu.stanford.mobisocial.dungbeetle;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.view.View;
import android.content.Intent;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.content.Context;
import android.util.Log;
import android.content.Intent;
import edu.stanford.mobisocial.dungbeetle.util.MyLocation;
import android.location.Location;
import android.app.ProgressDialog;

import edu.stanford.mobisocial.dungbeetle.google.*;

import android.net.Uri;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

public class NearbyGroupsActivity extends ListActivity {
    String[] listItems = {"sup dawg", "Restore from Dropbox", "Wipe Data (Keep identity)", "Start from Scratch"};
    ArrayList<GroupItem> groupList = new ArrayList<GroupItem>();
    
    String TAG = "Nearby Groups";


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


    public void onClickRefresh (View v)
    {
        
        locationClick();
    }

    public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

/*** End Dashboard Stuff ***/

    public MyLocation myLocation;
    public MyLocation.LocationResult locationResult;
    
    ProgressDialog dialog;

    private void locationClick() {
        dialog.show();
        groupList.clear();
        myLocation.getLocation(this, locationResult);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby_groups);
        setTitleFromActivityLabel (R.id.title_text);
        //setListAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems));
        final GroupAdapter adapter = new GroupAdapter(this, R.layout.nearby_groups_item, groupList);
        setListAdapter(adapter);
        dialog = new ProgressDialog(NearbyGroupsActivity.this);
        dialog.setMessage("Fetching nearby Groups. Please wait...");
        dialog.setCancelable(false);
        myLocation = new MyLocation();
        
        Intent intent = getIntent();

        final String password = intent.getStringExtra("password");
        
        locationResult = new MyLocation.LocationResult(){
            @Override
            public void gotLocation(final Location location){
                //Got the location!
                try {
                    Uri.Builder b = new Uri.Builder();
                    b.scheme("http");
                    b.authority("suif.stanford.edu");
                    b.path("dungbeetle/nearby.php");
                    Uri uri = b.build();
                    
                    StringBuffer sb = new StringBuffer();
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(uri.toString());

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                
                    nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
                    nameValuePairs.add(new BasicNameValuePair("lng", Double.toString(location.getLongitude())));
                    nameValuePairs.add(new BasicNameValuePair("password", password));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    try {
                        HttpResponse execute = client.execute(httpPost);
                        InputStream content = execute.getEntity().getContent();
                        BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                        String s = "";
                        while ((s = buffer.readLine()) != null) {
                            sb.append(s);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    String response = sb.toString();
                    JSONArray groupsJSON = new JSONArray(response);
                    for(int i = 0; i < groupsJSON.length(); i++) {
                        JSONObject group = new JSONObject(groupsJSON.get(i).toString());
                        groupList.add(new GroupItem(group.optString("group_name"), group.optString("feed_uri")));
                    }
                    adapter.notifyDataSetChanged();
                }
                catch(Exception e) {
                }

                
                dialog.dismiss();
            }
        };

        locationClick();
    }

    
    private class GroupItem {
        public String group_name, feed_uri;

        public GroupItem(String name, String uri) {
            group_name = name;
            feed_uri = uri;
        }
    }

    private class GroupAdapter extends ArrayAdapter<GroupItem> {
        private ArrayList<GroupItem> groups;
        
        public GroupAdapter(Context context, int textViewResourceId, ArrayList<GroupItem> groups) {
            super(context, textViewResourceId, groups);
            this.groups = groups;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = vi.inflate(R.layout.nearby_groups_item, null);
            }
            final GroupItem g = groups.get(position);
            TextView text = (TextView) row.findViewById(R.id.name_text);
            text.setText(g.group_name);

            row.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    Log.w(TAG, g.feed_uri);
                    Intent intent = new Intent(getApplicationContext(), HandleGroupSessionActivity.class);
                    intent.setData(Uri.parse(g.feed_uri));
                    startActivity(intent);
                }
            });
            return row;
        }
    }

}

