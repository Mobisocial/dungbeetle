package edu.stanford.mobisocial.dungbeetle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.group_providers.GroupProviders;
import edu.stanford.mobisocial.dungbeetle.model.Group;


public class GroupManagerThread extends Thread {
    public static final String TAG = "GroupManagerThread";
    public static final int UPDATE_INTERVAL_MS = 10000;
    private Context mContext;
    private DBHelper mHelper;
    private IdentityProvider mIdent;


    public GroupManagerThread(final Context context){
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
    }



    @Override
    public void run(){
        Log.i(TAG, "Running...");

        /* Update once every UPDATE_INTERVAL_MS milliseconds, 
         * while the screen is on. We also force updates 
         * when the group is initially created on a device.
         */

        while(!interrupted()) {
            if(App.instance().isScreenOn()){
                try {
                    try{
                        Cursor grps = mHelper.queryDynamicGroups();
                        Log.i(TAG, grps.getCount() + " dynamic groups...");
                        grps.moveToFirst();
                        while(!grps.isAfterLast()){
                            handleUpdate(new Group(grps));
                            grps.moveToNext();
                        }
                    }
                    catch(Exception e){
                        Log.wtf(TAG, e);
                    }
                    Thread.sleep(UPDATE_INTERVAL_MS);
                } catch(InterruptedException e) {}
            }
        }
        mHelper.close();
    }


    // FYI: Invoked in manager thread
    private void handleUpdate(final Group g){
        final Uri uri = Uri.parse(g.dynUpdateUri);
        final GroupRefreshHandler h = GroupProviders.forUri(uri);
        new Thread(){
            public void run(){
                h.handle(g.id, uri, mContext, mIdent);
            }
        }.start();
    }

    // These handlers should be stateless
    public interface GroupRefreshHandler{
        public boolean willHandle(Uri uri);
        public void handle(long id, Uri uri, Context context, IdentityProvider ident);
    }


}
