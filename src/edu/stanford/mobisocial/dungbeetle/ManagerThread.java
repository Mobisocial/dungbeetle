package edu.stanford.mobisocial.dungbeetle;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
import android.content.ContentValues;
import org.json.JSONException;
import edu.stanford.mobisocial.dungbeetle.model.Object;
import edu.stanford.mobisocial.bumblebee.OutgoingMessage;
import android.database.Cursor;
import java.security.PrivateKey;
import java.security.PublicKey;
import edu.stanford.mobisocial.bumblebee.TransportIdentityProvider;
import android.os.Message;
import edu.stanford.mobisocial.bumblebee.StateListener;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.bumblebee.MessageListener;
import edu.stanford.mobisocial.bumblebee.XMPPMessengerService;
import edu.stanford.mobisocial.bumblebee.MessengerService;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONObject;
import android.database.ContentObserver;
import android.net.Uri;
import android.content.Context;
import android.util.Log;
import android.os.Handler;

public class ManagerThread extends Thread {
    public static final String TAG = "ManagerThread";
    private Handler mToastHandler;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    private LinkedBlockingQueue<JSONObject> receiveQueue = 
        new LinkedBlockingQueue<JSONObject>();

    private LinkedBlockingQueue<JSONObject> sendQueue = 
        new LinkedBlockingQueue<JSONObject>();

    public ManagerThread(final Context context, final Handler toastHandler){
        mToastHandler = toastHandler;
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
        ConnectionStatus status = new ConnectionStatus(){
                public boolean isConnected(){
                    ConnectivityManager cm = 
                        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = cm.getActiveNetworkInfo();
                    return info != null && info.isConnected();
                }
            };
		mMessenger = new XMPPMessengerService(wrapIdent(mIdent), status);
		mMessenger.addStateListener(new StateListener() {
                public void onReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Connected to message transport!";
                    mToastHandler.sendMessage(m);
                }
                public void onNotReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Message transport not available.";
                    mToastHandler.sendMessage(m);
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage incoming) {
                    Log.i(TAG, "Got incoming message " + incoming);
                    handleIncomingMessage(incoming);
                }
            });

        mOco = new ObjectContentObserver(
            new Handler(mContext.getMainLooper()));

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me"), true, mOco);
    }


    private void handleIncomingMessage(IncomingMessage incoming){
        String contents = incoming.contents();
        Log.i(TAG, "Message contents: " + contents);
        String personId = incoming.from();
        Log.i(TAG, "Message from: " + personId);
        try{
            JSONObject obj = new JSONObject(contents);
            String feedName = obj.getString("feedName");
            mHelper.addExistingToFeed(personId, obj);
            mContext.getContentResolver().notifyChange(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName), null);
        }
        catch(JSONException e){
            Log.e(TAG, e.toString());
        }
    }


    @Override
    public void run(){
        Log.i(TAG, "Starting DungBeetle manager thread");
        Log.i(TAG, "Starting messenger...");
		mMessenger.init();
		for(;;) {
            if(mOco.changed){
                Log.i(TAG, "Noticed change...");
                mOco.clearChanged();
                Cursor objs = mHelper.queryRecentlyAdded(mIdent.userPersonId(), "friend");
                Log.i(TAG, objs.getCount() + " objects...");
                objs.moveToFirst();
                Cursor subscribers = mHelper.querySubscribers("friend");
                Log.i(TAG, subscribers.getCount() + " subscribers...");
                subscribers.moveToFirst();
                while(!objs.isAfterLast()){
                    while(!subscribers.isAfterLast()){
                        OutgoingMessage m = new OutgoingFeedObjectMsg(objs, subscribers);
                        mMessenger.sendMessage(m);
                        Log.i(TAG, "Sending message " + m);
                        subscribers.moveToNext();
                    }
                    objs.moveToNext();
                }
            }
			try {
				Thread.sleep(1000);
			} catch(InterruptedException e) {}
		}
    }


    private class OutgoingFeedObjectMsg implements OutgoingMessage{
        private String mBody;
        private PublicKey mPubKey;
        private String mToId;
        public OutgoingFeedObjectMsg(Cursor objs, Cursor subs){
            mToId = subs.getString(subs.getColumnIndexOrThrow("person_id"));
            mPubKey = mIdent.publicKeyForPersonId(mToId);
            mBody = objs.getString(objs.getColumnIndexOrThrow(Object.JSON));
        }
        public PublicKey toPublicKey(){ return mPubKey; }
        public String contents(){ return mBody; }
        public String toString(){ return "[Message to " + mToId + " with body: " + mBody + "]"; }
    }


    private TransportIdentityProvider wrapIdent(final IdentityProvider ident){
        return new TransportIdentityProvider(){
            public PublicKey userPublicKey(){
                return ident.userPublicKey();
            }
            public PrivateKey userPrivateKey(){
                return ident.userPrivateKey();
            }
            public String userPersonId(){
                return ident.userPersonId();
            }
            public PublicKey publicKeyForPersonId(String id){
                return ident.publicKeyForPersonId(id);
            }
            public String personIdForPublicKey(PublicKey key){
                return ident.personIdForPublicKey(key);
            }
        };
    }



    class ObjectContentObserver extends ContentObserver {
		public boolean changed;
		public ObjectContentObserver(Handler h)  {
			super(h);
			changed = false;
		}
		@Override
		public synchronized void onChange(boolean self) {
			changed = true;
			notify();
		}
		public synchronized void waitForChange() {
			if(changed)
				return;
			try {
				wait();
				changed = false;
			} catch(InterruptedException e) {}
		}
		public synchronized void clearChanged() {
			changed = false;
		}
	};


}