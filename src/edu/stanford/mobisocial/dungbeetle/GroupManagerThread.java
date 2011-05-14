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
    private Context mContext;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private ScreenState mScreenState;

    public GroupManagerThread(final Context context){
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenState = new ScreenState();
        mContext.registerReceiver(mScreenState, filter);
    }



    @Override
    public void run(){
        Log.i(TAG, "Starting DungBeetle group manager thread");
        while(!interrupted()) {
            if(!mScreenState.isOff){
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

                    Thread.sleep(15000);
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

    public class ScreenState extends BroadcastReceiver {
        public boolean isOff = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isOff = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                isOff = false;
            }
        }

    }

}
