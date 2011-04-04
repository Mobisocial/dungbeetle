package edu.stanford.mobisocial.dungbeetle;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;

public class ViewContactActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String contactId = (String) this.getIntent().getExtras().get("contact_id");
        String outputText = "";
        
        Cursor phones = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null); 
        while (phones.moveToNext()) { 
           String phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));                 
           outputText += phoneNumber + "\n";
        } 
     phones.close();
        
        
        TextView textview = new TextView(this);
        textview.setText(outputText);
        
        setContentView(textview);
        
    	/*try {
			   Intent intent = new Intent(Intent.ACTION_CALL);
			   intent.setData(Uri.parse("tel:+436641234567"));
			   startActivity(intent);
		} catch (Exception e) {
			   Log.e("SampleApp", "Failed to invoke call", e);
		}*/
    }
}