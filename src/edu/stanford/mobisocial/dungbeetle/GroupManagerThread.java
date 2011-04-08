package edu.stanford.mobisocial.dungbeetle;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import java.util.ArrayList;
import java.util.List;


public class GroupManagerThread extends Thread {
    public static final String TAG = "GroupManagerThread";
    private Context mContext;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    public GroupManagerThread(final Context context){
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
        mOco = new ObjectContentObserver(new Handler(mContext.getMainLooper()));
		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                      "/dynamic_groups"), true, mOco);
        mHandlers.add(new PrplGroupHandler());
    }

    @Override
    public void run(){
        Log.i(TAG, "Starting DungBeetle group manager thread");
        while(!interrupted()) {
            try{
                if(mOco.changed){
                    // Do we need this?
                    Log.i(TAG, "Noticed change...");
                    mOco.clearChanged();
                }
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

            try {
                Thread.sleep(10000);
            } catch(InterruptedException e) {}
        }
        mHelper.close();
    }

    class ObjectContentObserver extends ContentObserver {
        public boolean changed;
        public ObjectContentObserver(Handler h)  {
            super(h);
            // Default to true so we do the initial check.
            changed = true; 
        }
        @Override
        public synchronized void onChange(boolean self) {
            changed = true;
            notify();
        }
        public synchronized void clearChanged() {
            changed = false;
        }
    };

    private List<GroupHandler> mHandlers = new ArrayList<GroupHandler>();

    // FYI: Invoked in manager thread
    private void handleUpdate(final Group g){
        final Uri uri = Uri.parse(g.dynUpdateUri);
        for(final GroupHandler h : mHandlers){
            if(h.willHandle(uri)){
                new Thread(){public void run(){
                    h.handle(g.id, uri);
                }}.start();
                break;
            }
        }
    }

    // These handlers should be stateless
    abstract class GroupHandler{
        abstract boolean willHandle(Uri uri);
        abstract void handle(long id, Uri uri);
    }

    class PrplGroupHandler extends GroupHandler{
        public boolean willHandle(Uri uri){
            return uri.getAuthority().equals("suif.stanford.edu");
        }
        public void handle(long id, Uri uriIn){
            Uri.Builder b = uriIn.buildUpon();
            b.scheme("http");
            Uri uri = b.build();
            Log.i(TAG, "Doing dynamic group update for " + uri);
        }
    }

}
