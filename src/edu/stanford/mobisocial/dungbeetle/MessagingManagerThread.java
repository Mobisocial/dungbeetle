package edu.stanford.mobisocial.dungbeetle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.TVModePresence;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.obj.handler.AutoActivateObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.FeedModifiedObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IteratorObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.NotificationObjHandler;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.StringSearchAndReplacer;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class MessagingManagerThread extends Thread {
    public static final String TAG = "MessagingManagerThread";
    public static final boolean DBG = true;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private Handler mMainThreadHandler;
    private final Set<Long>mSentObjects = new HashSet<Long>();
    private final FeedModifiedObjHandler mFeedModifiedObjHandler;

    public MessagingManagerThread(final Context context){
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mHelper = new DBHelper(context);
        mIdent = new DBIdentityProvider(mHelper);
        mFeedModifiedObjHandler = new FeedModifiedObjHandler(mHelper);

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
                    if (DBG) Log.i(TAG, "Got incoming message " + incoming);
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
   
        if (DBG) Log.i(TAG, "Localized contents: " + contents);
        try {
            final JSONObject obj = new JSONObject(contents);
            String feedName = obj.getString("feedName");
            String type = obj.optString(DbObjects.TYPE);
            Log.w(TAG, "encoded length: " + encoded.length);
            //if (encoded.length > 200000 || mHelper.queryAlreadyReceived(encoded)) {
            if (mHelper.queryAlreadyReceived(encoded)) {
                if (DBG) Log.i(TAG, "Message already received. " + contents);
                return;
            }

            Maybe<Contact> contact = mHelper.contactForPersonId(personId);
            DbEntryHandler h = DbObjects.getIncomingMessageHandler(obj);
            if (h != null && h instanceof UnprocessedMessageHandler) {
                ((UnprocessedMessageHandler)h).handleUnprocessed(mContext, obj);
            }

            if (contact.isKnown()) {
                long sequenceID;
                final Contact realContact = contact.get();
                long contactId = realContact.id;
                if (DBG) Log.d(TAG, "Msg from " + contactId + " ( " + realContact.name  + ")");
                // Insert into the database. (TODO: Handler, both android.os and musubi.core)
				sequenceID = mHelper.addObjectByJson(contact.otherwise(Contact.NA()).id, obj, encoded);
				Uri feedUri;
                if (feedName.equals("friend")) {
                   feedUri = Feed.uriForName("friend/" + contactId);
                } else {
                    feedUri = Feed.uriForName(feedName);
                }
                mContext.getContentResolver().notifyChange(feedUri, null);
                if (feedName.equals("direct") || feedName.equals("friend")) {
                    // TODO: Is this threading necessary?
                    mMainThreadHandler.post(new Runnable() {
                        public void run() {
                            long time = obj.optLong(DbObject.TIMESTAMP);
                            Helpers.updateLastPresence(mContext, realContact, time);

                            final DbEntryHandler h = DbObjects.getIncomingMessageHandler(obj);
                            if (h != null) {
                                h.handleDirectMessage(mContext, realContact, obj);
                            }
                        }
                    });
                }

                /**
                 * Run handlers over all received objects:
                 */

                // TODO: framework code.
                getFromNetworkHandlers().handleObj(mContext, feedUri, realContact, sequenceID,
                        DbObjects.forType(type), obj);

                // Per-object handlers:
                if (h != null && h instanceof FeedMessageHandler) {
                    ((FeedMessageHandler) h).handleFeedMessage(
                            mContext, feedUri, contactId, sequenceID, type, obj);
                }
            } else {
                Log.i(TAG, "Message from unknown contact. " + contents);
            }
        }
        catch(Exception e){
            Log.e(TAG, "Error handling incoming message.", e);
        }
    }

    private IteratorObjHandler mFromNetworkHandlers;
    public IteratorObjHandler getFromNetworkHandlers() {
        if (mFromNetworkHandlers == null) {
            mFromNetworkHandlers = new IteratorObjHandler();
            mFromNetworkHandlers.addHandler(TVModePresence.getInstance());
            mFromNetworkHandlers.addHandler(Push2TalkPresence.getInstance());
            mFromNetworkHandlers.addHandler(new AutoActivateObjHandler());
            mFromNetworkHandlers.addHandler(new NotificationObjHandler(mHelper));
            mFromNetworkHandlers.addHandler(mFeedModifiedObjHandler);
        }
        return mFromNetworkHandlers;
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
    public void run() {
        Set<Long> notSendingObjects = new HashSet<Long>();
        if (DBG) Log.i(TAG, "Running...");
        mMessenger.init();
        while (!interrupted()) {
            mOco.waitForChange();
            if (DBG) Log.i(TAG, "Noticed change...");
            mOco.clearChanged();
            Cursor objs = mHelper.queryUnsentObjects();
            try {
                if (DBG) Log.i(TAG, objs.getCount() + " objects...");
                objs.moveToFirst();
                while (!objs.isAfterLast()) {
                    Long objId = objs.getLong(objs.getColumnIndexOrThrow(DbObject._ID));
                    String feedName = objs.getString(objs.getColumnIndexOrThrow(DbObject.FEED_NAME));
                    String jsonSrc = objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON));
                    Uri feedUri = Feed.uriForName(feedName);
                    JSONObject json = null;
                    try {
                        json = new JSONObject(jsonSrc);
                    } catch (JSONException e) {
                        Log.e(TAG, "bad json", e);
                    }
                    if (json != null) {
                        String type = json.optString(DbObjects.TYPE);
                        /*if you udpate latest feed here then there is a race condition between
                         * when you put a message into your db,
                         * when you actually have a connection to send the message (which is here)
                         * when other people send you messages
                         * the processing gets all out of order, so instead we update latest
                         * immediately when you add messages into your db inside
                         * DBHelper.java addToFeed();
                         */
                        //mFeedModifiedObjHandler.handleObj(mContext, feedUri, objId);
                        DbEntryHandler h = DbObjects.getIncomingMessageHandler(json);
                        if (h != null && h instanceof FeedMessageHandler) {
                            ((FeedMessageHandler) h).handleFeedMessage(
                                    mContext, feedUri, Contact.MY_ID, -1, type, json);
                        }
                    }
                    if (mSentObjects.contains(objId)) {
                        if (DBG) Log.i(TAG, "Skipping previously sent object " + objId);
                        objs.moveToNext();
                        continue;
                    }
                    String to = objs.getString(objs.getColumnIndexOrThrow(DbObject.DESTINATION));
                    if (to != null) {
                        OutgoingMessage m = new OutgoingDirectObjectMsg(objs);
                        if (DBG) Log.i(TAG, "Sending direct message " + m);
                        if (m.toPublicKeys().isEmpty()) {
                            Log.w(TAG, "No addressees for direct message " + objId);
                            notSendingObjects.add(objId);
                        } else {
                            synchronized (mSentObjects) {
                                mSentObjects.add(objId);
                            }
                            mMessenger.sendMessage(m);
                        }
                    } else {
                        OutgoingMessage m = new OutgoingFeedObjectMsg(objs);
                        if (DBG) Log.i(TAG, "Sending feed object " + objId + ": " + m);
                        if(m.toPublicKeys().isEmpty()) {
                            Log.i(TAG, "No addresses for feed message " + objId);
                            notSendingObjects.add(objId);
                        } else {
                            synchronized (mSentObjects) {
                                mSentObjects.add(objId);
                            }
                            mMessenger.sendMessage(m);
                        }
                    }
                    objs.moveToNext();
                }
                if (notSendingObjects.size() > 0) {
                    if (DBG) Log.d(TAG, "Marking " + notSendingObjects.size() + " objects sent");
                    mHelper.markObjectsAsSent(notSendingObjects);
                    notSendingObjects.clear();
                }
            } catch (Exception e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                    Log.wtf(TAG, "error running notify loop", e);
                } else {
                    Log.e(TAG, "error running notify loop", e);
                }
            } finally {
                objs.close();
            }
        }
        mHelper.close();
    }

    private abstract class OutgoingMsg implements OutgoingMessage {
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
            if (mSentObjects.contains(mObjectId)) {
                synchronized (mSentObjects) {
                    mSentObjects.remove(mObjectId);
                }
            }
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

    private class OutgoingFeedObjectMsg extends OutgoingMsg {
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

    private class OutgoingDirectObjectMsg extends OutgoingMsg {
        public OutgoingDirectObjectMsg(Cursor objs) {
            super(objs);
            String to = objs.getString(objs.getColumnIndexOrThrow(DbObject.DESTINATION));
            try {
                List<Long> ids = Util.splitLongsToList(to, ",");
                mPubKeys = mIdent.publicKeysForContactIds(ids);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Bad destination found: '" + to + "'");
                mPubKeys = new ArrayList<RSAPublicKey>();
            }
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
}
