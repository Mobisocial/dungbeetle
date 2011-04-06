package edu.stanford.mobisocial.dungbeetle;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
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
    private Handler mDirectMessageHandler;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    private LinkedBlockingQueue<JSONObject> receiveQueue = 
        new LinkedBlockingQueue<JSONObject>();

    private LinkedBlockingQueue<JSONObject> sendQueue = 
        new LinkedBlockingQueue<JSONObject>();

    public ManagerThread(final Context context, 
                         final Handler toastHandler, 
                         final Handler directMessageHandler){
        mToastHandler = toastHandler;
        mDirectMessageHandler = directMessageHandler;
        mContext = context;
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
        ConnectionStatus status = new ConnectionStatus(){
                public boolean isConnected(){
                    ConnectivityManager cm = 
                        (ConnectivityManager)context.getSystemService(
                            Context.CONNECTIVITY_SERVICE);
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

        mOco = new ObjectContentObserver(new Handler(mContext.getMainLooper()));

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me"), true, mOco);

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out"), true, mOco);
    }


    private void handleIncomingMessage(IncomingMessage incoming){
        String contents = incoming.contents();
        Log.i(TAG, "Message contents: " + contents);
        String personId = incoming.from();
        Log.i(TAG, "Message from: " + personId);
        try{
            JSONObject obj = new JSONObject(contents);
            String feedName = obj.getString("feedName");
            long contactId = mIdent.contactIdForPersonId(personId);
            mHelper.addObjectByJson(contactId, obj);
            mContext.getContentResolver().notifyChange(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/" + feedName), 
                null);

            if(feedName.equals("direct")){
                Message m = mDirectMessageHandler.obtainMessage();
                m.obj = incoming;
                mDirectMessageHandler.sendMessage(m);
            }

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
        while(!interrupted()) {
            if(mOco.changed){
                Log.i(TAG, "Noticed change...");
                mOco.clearChanged();
                Cursor objs = mHelper.queryRecentlyAdded();
                Log.i(TAG, objs.getCount() + " objects...");
                objs.moveToFirst();
                while(!objs.isAfterLast()){
                    String to = objs.getString(
                        objs.getColumnIndexOrThrow(Object.DESTINATION));
                    if(to != null){
                        OutgoingMessage m = new OutgoingDirectObjectMsg(objs);
                        Log.i(TAG, "Sending direct message " + m);
                        Log.i(TAG, "Sending to " + to);
                        if(m.toPublicKeys().isEmpty()){
                            Log.e(TAG, "Empty addressees!");
                        }
                        mMessenger.sendMessage(m);
                    }
                    else{
                        OutgoingMessage m = new OutgoingFeedObjectMsg(objs);
                        if(m.toPublicKeys().isEmpty()){
                            Log.e(TAG, "Empty addressees!");
                        }
                        mMessenger.sendMessage(m);
                    }
                    objs.moveToNext();
                }
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {}
        }
        mHelper.close();
    }

    private abstract class OutgoingMsg implements OutgoingMessage{
        protected String mBody;
        protected List<PublicKey> mPubKeys;
        public List<PublicKey> toPublicKeys(){ return mPubKeys; }
        public String contents(){ return mBody; }
        public String toString(){ return "[Message with body: " + mBody + " to " + toPublicKeys().size() + " recipient(s) ]"; }
    }

    private class OutgoingFeedObjectMsg extends OutgoingMsg{
        public OutgoingFeedObjectMsg(Cursor objs){
            String feedName = objs.getString(
                objs.getColumnIndexOrThrow(Object.FEED_NAME));
            Cursor subs = mHelper.querySubscribers(feedName);
            subs.moveToFirst();
            ArrayList<Long> ids = new ArrayList<Long>();
            while(!subs.isAfterLast()){
                ids.add(subs.getLong(
                            subs.getColumnIndexOrThrow(Subscriber.CONTACT_ID)));
                subs.moveToNext();
            }
            mPubKeys = mIdent.publicKeysForContactIds(ids);
            mBody = objs.getString(objs.getColumnIndexOrThrow(Object.JSON));
        }
    }

    private class OutgoingDirectObjectMsg extends OutgoingMsg{
        public OutgoingDirectObjectMsg(Cursor objs){
            String to = objs.getString(
                objs.getColumnIndexOrThrow(Object.DESTINATION));
            String[] tos = to.split(",");
            Long[] ids = new Long[tos.length];
            for(int i = 0; i < tos.length; i++) ids[i] = Long.valueOf(tos[i]);
            mPubKeys = mIdent.publicKeysForContactIds(Arrays.asList(ids));
            mBody = objs.getString(objs.getColumnIndexOrThrow(Object.JSON));
        }
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