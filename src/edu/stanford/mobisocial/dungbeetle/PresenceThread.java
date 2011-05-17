package edu.stanford.mobisocial.dungbeetle;
import android.content.Context;
import android.util.Log;
import java.lang.InterruptedException;

public class PresenceThread extends Thread {
    public static final String TAG = "PresenceThread";
    private Context mContext;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    public PresenceThread(final Context context){
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
    }

    @Override
    public void run(){
        Log.i(TAG, "Running...");
        while(!interrupted()) {
            try{
                Thread.sleep(10000);
            } catch(InterruptedException e) {}
        }
        mHelper.close();
    }


}
