package edu.stanford.mobisocial.dungbeetle;
import android.content.DialogInterface;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.Button;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import android.widget.CursorAdapter;
import android.net.Uri;
import android.database.Cursor;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.view.View.OnClickListener;


public class ObjectsActivity extends ListActivity implements OnItemClickListener{

	private ObjectListCursorAdapter mObjects;
	private DBIdentityProvider mIdent;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"),
            new String[]{Object._ID, Object.JSON },
            null, null, Object.TIMESTAMP + " DESC");
		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
 
        mIdent = new DBIdentityProvider(new DBHelper(this));

		Button button = (Button)findViewById(R.id.add_object_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(ObjectsActivity.this);
                    alert.setMessage("Please enter your new status message:");
                    final EditText input = new EditText(ObjectsActivity.this);
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String value = input.getText().toString();
                                updateStatus(value);
                            }
                        });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                    alert.show();

				}
			});
	}


    private void updateStatus(final String status){
        Uri url = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        ContentValues values = new ContentValues();
        JSONObject obj = new JSONObject();
        try{
            obj.put("text", status);
            obj.put("name", mIdent.userName());
        }catch(JSONException e){}
        values.put("json", obj.toString());
        values.put("type", "status");
        getContentResolver().insert(url, values); 
    }


	public void onItemClick(AdapterView<?> parent, View view, int position, long id){}

    private class ObjectListCursorAdapter extends CursorAdapter {

        public ObjectListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.objects_item, parent, false);
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String name = obj.optString("name");
                String text = obj.optString("text");
                TextView nameText = (TextView) v.findViewById(R.id.name_text);
                if (nameText != null) {
                    nameText.setText(name + ": '" + text + "'");
                }
            }catch(JSONException e){}
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String jsonSrc = c.getString(c.getColumnIndexOrThrow(Object.JSON));
            try{
                JSONObject obj = new JSONObject(jsonSrc);
                String name = obj.optString("name");
                String text = obj.optString("text");
                TextView nameText = (TextView) v.findViewById(R.id.name_text);
                if (nameText != null) {
                    nameText.setText(name + ": '" + text + "'");
                }
            }catch(JSONException e){}
        }

    }


}



