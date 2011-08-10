package edu.stanford.mobisocial.dungbeetle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
import edu.stanford.mobisocial.bumblebee.ConnectionStatusListener;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.bumblebee.MessageListener;
import edu.stanford.mobisocial.bumblebee.MessengerService;
import edu.stanford.mobisocial.bumblebee.OutgoingMessage;
import edu.stanford.mobisocial.bumblebee.RabbitMQMessengerService;
import edu.stanford.mobisocial.bumblebee.StateListener;
import edu.stanford.mobisocial.bumblebee.TransportIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.StringSearchAndReplacer;
import edu.stanford.mobisocial.dungbeetle.util.Util;


public class MessagingManagerThread extends Thread {
    public static final String TAG = "MessagingManagerThread";
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private Handler mMainThreadHandler;

    public MessagingManagerThread(final Context context){
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
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
		mMessenger = new RabbitMQMessengerService(wrapIdent(mIdent), status);
		mMessenger.addStateListener(new StateListener() {
                public void onReady() {
                    Log.i(TAG, "Connected to message transport!");
                }
                public void onNotReady() {
                    Log.i(TAG, "Message transport not available.");
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage incoming) {
                    Log.i(TAG, "Got incoming message " + incoming);
                    handleIncomingMessage(incoming);
                }
            });        
		mMessenger.addConnectionStatusListener(new ConnectionStatusListener() {
			
			@Override
			public void onStatus(String msg, Exception e) {
				StringWriter err = new StringWriter();
				PrintWriter p = new PrintWriter(err);
				if(e != null) {
					p.println(e.toString());
					p.println(e.getMessage());
					e.printStackTrace(p);
				}
				
                Log.e(TAG, "Connection Status: " + msg + "\n" + err.toString());
			}
		});

        mOco = new ObjectContentObserver(new Handler(mContext.getMainLooper()));

		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds"), true, mOco);
		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/out"), true, mOco);
    }

    // FYI: Invoked on connection reader thread
    private void handleIncomingMessage(final IncomingMessage incoming){
        final String personId = incoming.from();
        final byte[] encoded = incoming.encoded();
        final String contents = localize(incoming.contents());
        final IncomingMessage localizedMsg = new IncomingMessage(){
                public String contents(){ return contents; }
                public String from(){ return personId; }
                public byte[] encoded() { return encoded; }
            };
        Log.i(TAG, "Localized contents: " + contents);
        try {
            JSONObject obj = new JSONObject(contents);
            String feedName = obj.getString("feedName");
            Maybe<Contact> contact = mHelper.contactForPersonId(personId);
            if (mHelper.queryAlreadyReceived(encoded)) {
                Log.i(TAG, "Message already received. " + contents);
                return;
            }

            DbEntryHandler h = DbObjects.getIncomingMessageHandler(obj);
            if (h != null && h instanceof UnprocessedMessageHandler) {
                ((UnprocessedMessageHandler)h).handleUnprocessed(mContext, obj);
            }

            if (contact.isKnown()) {
				mHelper.addObjectByJson(contact.otherwise(Contact.NA()).id, obj, encoded);
                mContext.getContentResolver().notifyChange(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + 
                              "/feeds/" + feedName), null);
                if(feedName.equals("direct") || feedName.equals("friend")){
                    mMainThreadHandler.post(new Runnable(){
                            public void run(){
                                handleSpecialMessage(localizedMsg);
                            }
                        });
                }
            } else {
                Log.i(TAG, "Message from unknown contact. " + contents);
            }
        }
        catch(Exception e){
            Log.e(TAG, "Error handling incoming message: " + e.toString());
        }
    }


    /**
     * Replace global contact and object references 
     * with their local analogs.
     */
    private String localize(String s){
        return mContactLocalizer.apply(s);
    }


    /**
     * Replace local contact and object ids 
     * with their global analogs.
     */
    private String globalize(String s){
        return mContactGlobalizer.apply(s);
    }


    // Note, doing a query on each replacement is not friendly : (
    // Should use a contact cache here.

    private StringSearchAndReplacer mContactGlobalizer = new StringSearchAndReplacer("\"@l([\\-0-9]+)\""){
            protected String replace(Matcher m){
                Long id = Long.valueOf(m.group(1));
                String personId;
                if(id.equals(Contact.MY_ID)){
                    personId = mIdent.userPersonId();
                }
                else{
                    Maybe<Contact> c = mHelper.contactForContactId(id);
                    personId = c.otherwise(Contact.NA()).personId;
                }
                return "\"@g" + personId + "\"";
            }
        };

    private StringSearchAndReplacer mContactLocalizer = new StringSearchAndReplacer("\"@g([^\"]+)\""){
            protected String replace(Matcher m){
                String personId = m.group(1);
                if(personId.equals(mIdent.userPersonId())){
                    return String.valueOf(Contact.MY_ID);
                }
                else{
                    Maybe<Contact> c = mHelper.contactForPersonId(personId);
                    return String.valueOf(c.otherwise(Contact.NA()).id);
                }
            }
        };


    @Override
    public void run(){
        Log.i(TAG, "Running...");
        mMessenger.init();
        while(!interrupted()) {
            try{
                mOco.waitForChange();
                Log.i(TAG, "Noticed change...");
                mOco.clearChanged();
                Cursor objs = mHelper.queryUnsentObjects();
                Log.i(TAG, objs.getCount() + " objects...");
                objs.moveToFirst();
                ArrayList<Long> sent = new ArrayList<Long>();
                while(!objs.isAfterLast()){
                    String to = objs.getString(
                        objs.getColumnIndexOrThrow(DbObject.DESTINATION));
                    if(to != null){
                        OutgoingMessage m = new OutgoingDirectObjectMsg(objs);
                        Log.i(TAG, "Sending direct message " + m);
                        if(m.toPublicKeys().isEmpty()){
                            Log.e(TAG, "Empty addressees!");
                        }
                        mMessenger.sendMessage(m);
                    }
                    else{
                        OutgoingMessage m = new OutgoingFeedObjectMsg(objs);
                        Log.i(TAG, "Sending feed object " + m);
                        if(m.toPublicKeys().isEmpty()){
                            Log.e(TAG, "Empty addressees!");
                        }
                        mMessenger.sendMessage(m);
                    }
                    sent.add(objs.getLong(objs.getColumnIndexOrThrow(DbObject._ID)));
                    objs.moveToNext();
                }
                objs.close();
            }
            catch(Exception e){
                Log.e(TAG, "wtf", e);
            }
        }
        mHelper.close();
    }

    private abstract class OutgoingMsg implements OutgoingMessage{
        protected String mBody;
        protected List<RSAPublicKey> mPubKeys;
        protected long mObjectId;
        protected byte[] mEncoded;
        protected OutgoingMsg(Cursor objs) {
        	mObjectId = objs.getLong(0 /*DbObject._ID*/);
        	//load the iv if it was already picked
        	int encoded_index = objs.getColumnIndexOrThrow(DbObject.ENCODED);
        	mEncoded = objs.getBlob(encoded_index);        	
        }
		@Override
		public long getLocalUniqueId() {
			return mObjectId;
		}
        public List<RSAPublicKey> toPublicKeys(){ return mPubKeys; }
        public String contents(){ return mBody; }
        public String toString(){ return "[Message with body: " + mBody + " to " + toPublicKeys().size() + " recipient(s) ]"; }
        public void onCommitted() {
        	mHelper.markObjectAsSent(mObjectId);
        }

		@Override
		public void onEncoded(byte[] encoded) {
			mEncoded = encoded;
			mHelper.markEncoded(mObjectId, encoded);
		}

		@Override
		public byte[] getEncoded() {
			return mEncoded;
		}
    }

    private class OutgoingFeedObjectMsg extends OutgoingMsg{
    	
        public OutgoingFeedObjectMsg(Cursor objs){
        	super(objs);
            String feedName = objs.getString(
                objs.getColumnIndexOrThrow(DbObject.FEED_NAME));
            Cursor subs = mHelper.querySubscribers(feedName);
            subs.moveToFirst();
            ArrayList<Long> ids = new ArrayList<Long>();
            while(!subs.isAfterLast()){
                ids.add(subs.getLong(
                            subs.getColumnIndexOrThrow(Subscriber.CONTACT_ID)));
                subs.moveToNext();
            }
            mPubKeys = mIdent.publicKeysForContactIds(ids);
            mBody = globalize(objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON)));
        }

    }

    private class OutgoingDirectObjectMsg extends OutgoingMsg{
        public OutgoingDirectObjectMsg(Cursor objs){
        	super(objs);
            String to = objs.getString(
                objs.getColumnIndexOrThrow(DbObject.DESTINATION));
            List<Long> ids = Util.splitLongsToList(to, ",");
            mPubKeys = mIdent.publicKeysForContactIds(ids);
            mBody = globalize(objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON)));
        }
    }

    private TransportIdentityProvider wrapIdent(final IdentityProvider ident){
        return new TransportIdentityProvider(){
            public RSAPublicKey userPublicKey(){
                return ident.userPublicKey();
            }
            public RSAPrivateKey userPrivateKey(){
                return ident.userPrivateKey();
            }
            public String userPersonId(){
                return ident.userPersonId();
            }
            public RSAPublicKey publicKeyForPersonId(String id){
                return ident.publicKeyForPersonId(id);
            }
            public String personIdForPublicKey(RSAPublicKey key){
                return ident.personIdForPublicKey(key);
            }
        };
    }


    class ObjectContentObserver extends ContentObserver {
		public boolean changed;
		public ObjectContentObserver(Handler h)  {
			super(h);
			changed = true;
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


    // FYI: Must be invoked from main app thread. See above.
    private void handleSpecialMessage(IncomingMessage incoming){
        String contents = incoming.contents();
        try{
            final Contact c = mHelper.contactForPersonId(incoming.from()).get();
            try{
                JSONObject obj = new JSONObject(contents);
                long time = obj.optLong(DbObject.TIMESTAMP);
                Helpers.updateLastPresence(mContext, c, time);

                final DbEntryHandler h = DbObjects.getIncomingMessageHandler(obj);
                if(h != null) {
                    h.handleReceived(mContext, c, obj);
                }
            }
            catch(JSONException e){ throw new RuntimeException(e); }
        }
        catch(Maybe.NoValError e){
            Log.e(TAG, "Oops, no contact for message " + contents);
        }
    }
}
