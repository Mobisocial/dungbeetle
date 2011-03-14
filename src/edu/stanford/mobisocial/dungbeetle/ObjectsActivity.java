package edu.stanford.mobisocial.dungbeetle;
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


public class ObjectsActivity extends ListActivity implements OnItemClickListener{

	private ObjectListCursorAdapter mObjects;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.objects);
        Cursor c = getContentResolver().query(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/all"), 
            new String[]{Object._ID, Object.JSON }, 
            null, null, null);
		mObjects = new ObjectListCursorAdapter(this, c);
		setListAdapter(mObjects);
		getListView().setOnItemClickListener(this);
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
		// JSONObject o = mObjects.getItem(position);
		// String userId = o.optString("id");
		// Intent intent = new Intent(ViewProfileActivity.LAUNCH_INTENT);
		// intent.putExtra("user_id", userId);
		// startActivity(intent);
	}


    private class ObjectListCursorAdapter extends CursorAdapter {

        public ObjectListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.objects_item, parent, false);
            String name = c.getString(c.getColumnIndexOrThrow(Object.JSON));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }
            return v;
        }


        @Override
        public void bindView(View v, Context context, Cursor c) {
            String name = c.getString(c.getColumnIndexOrThrow(Object.JSON));
            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            if (nameText != null) {
                nameText.setText(name);
            }
        }

    }


}



