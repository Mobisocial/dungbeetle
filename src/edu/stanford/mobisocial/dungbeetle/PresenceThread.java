package edu.stanford.mobisocial.dungbeetle;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class PresenceThread extends Thread {
    public static final String TAG = "PresenceThread";
    private Context mContext;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    public PresenceThread(final Context context){
        mContext = context;
        mHelper = DBHelper.getGlobal(context);
        mIdent = new DBIdentityProvider(mHelper);
    }

    @Override
    public void run(){
        Log.i(TAG, "Running...");
        while(!interrupted()) {
            try{
                // Cursor c = mHelper.sampleFromContacts(10);
                // c.moveToFirst();
                // while(!c.isAfterLast()){
                    
                //     c.moveToNext();

                // }
                if(App.instance().isScreenOn()){
                    Thread.sleep(10000);
                }
                else{
                    Thread.sleep(60000);
                }
            }
            catch(Exception e){
                Log.wtf(TAG, e);
            }
        }
        mHelper.close();
    }


}
