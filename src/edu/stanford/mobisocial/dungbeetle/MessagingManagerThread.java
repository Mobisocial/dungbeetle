package edu.stanford.mobisocial.dungbeetle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import mobisocial.socialkit.musubi.DbObj;

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
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import edu.stanford.mobisocial.bumblebee.ConnectionStatus;
import edu.stanford.mobisocial.bumblebee.ConnectionStatusListener;
import edu.stanford.mobisocial.bumblebee.CryptoException;
import edu.stanford.mobisocial.bumblebee.IncomingMessage;
import edu.stanford.mobisocial.bumblebee.MessageFormat;
import edu.stanford.mobisocial.bumblebee.MessageListener;
import edu.stanford.mobisocial.bumblebee.MessengerService;
import edu.stanford.mobisocial.bumblebee.OutgoingMessage;
import edu.stanford.mobisocial.bumblebee.RabbitMQMessengerService;
import edu.stanford.mobisocial.bumblebee.StateListener;
import edu.stanford.mobisocial.bumblebee.TransportIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.OutgoingMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.UnprocessedMessageHandler;
import edu.stanford.mobisocial.dungbeetle.feed.presence.DropMessagesPresence.MessageDropHandler;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;
import edu.stanford.mobisocial.dungbeetle.feed.presence.TVModePresence;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.model.Subscriber;
import edu.stanford.mobisocial.dungbeetle.obj.handler.AutoActivateObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.IteratorObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.NotificationObjHandler;
import edu.stanford.mobisocial.dungbeetle.obj.handler.ProfileScanningObjHandler;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.StringSearchAndReplacer;
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class MessagingManagerThread extends Thread {
    public static final String TAG = "MessagingManagerThread";
    public static final boolean DBG = false;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private final MessageDropHandler mMessageDropHandler;

    private static final String JSON_INT_KEY = "obj_intkey";

    public MessagingManagerThread(final Context context){
        mContext = context;
        mHelper = DBHelper.getGlobal(context);
        mIdent = new DBIdentityProvider(mHelper);
        mMessageDropHandler = new MessageDropHandler();

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
        final long hash = incoming.hash();
        final String contents = localize(incoming.contents());
   
        /**
         * TODO: This needs to be updated with the POSI standards
         * to accept a SignedObj.
         */

        if (DBG) Log.i(TAG, "Localized contents: " + contents);
        try {
            JSONObject in_obj = new JSONObject(contents);
            String feedName = in_obj.getString("feedName");
            String type = in_obj.optString(DbObjects.TYPE);
            Uri feedPreUri = Feed.uriForName(feedName);
            if (mMessageDropHandler.preFiltersObj(mContext, feedPreUri)) {
                return;
            }

            if (mHelper.queryAlreadyReceived(hash)) {
                if (DBG) Log.i(TAG, "Message already received. " + contents);
                return;
            }

            Maybe<Contact> contact = mHelper.contactForPersonId(personId);
            final DbEntryHandler objHandler = DbObjects.getObjHandler(in_obj);
            byte[] extracted_data = null;
            if (objHandler instanceof UnprocessedMessageHandler) {
            	Pair<JSONObject, byte[]> r =((UnprocessedMessageHandler)objHandler).handleUnprocessed(mContext, in_obj);
            	if(r != null) {
            		in_obj = r.first;
            		extracted_data = r.second;
            	}
            }
            final JSONObject obj = in_obj;
            final byte[] raw = extracted_data;

            /**
             *  TODO STFAN BJDODSON KANAKB
             *
             *  See FriendAcceptObj.handleUnprocessed as template, code is something like:
             *
             *  if (!mPublicKeyDirectory.verify(in_obj.getString("email"), in_obj.getPublicKey())) {
             *    Log.w("Spammer trying to claim public key for email address");
             *    return;
             *  }
             *  if (inAddressBook(email)) {
             *     // auto-accept and notify of new friend
             *  } else {
             *     // notification to accept friend
             *  }
             */

            if (!contact.isKnown()) {
                Log.i(TAG, "Message from unknown contact. " + contents);
                return;
            }

            long objId;
            final Contact realContact = contact.get();
            long contactId = realContact.id;
            if (DBG) Log.d(TAG, "Msg from " + contactId + " ( " + realContact.name  + ")");
            // Insert into the database. (TODO: Handler, both android.os and musubi.core)

            if (!objHandler.handleObjFromNetwork(mContext, realContact, obj)) {
                return;
            }

            Integer intKey = null;
            if (obj.has(JSON_INT_KEY)) {
                intKey = obj.getInt(JSON_INT_KEY);
                obj.remove(JSON_INT_KEY);
            }
            objId = mHelper.addObjectByJson(contact.otherwise(Contact.NA()).id, obj, hash, raw, intKey);
			Uri feedUri;
            if (feedName.equals("friend")) {
               feedUri = Feed.uriForName("friend/" + contactId);
            } else {
                feedUri = Feed.uriForName(feedName);
            }
            mContext.getContentResolver().notifyChange(feedUri, null);
            if (feedName.equals("direct") || feedName.equals("friend")) {
                long time = obj.optLong(DbObject.TIMESTAMP);
                Helpers.updateLastPresence(mContext, realContact, time);
                objHandler.handleDirectMessage(mContext, realContact, obj);
            }

            /**
             * Run handlers over all received objects:
             */

            // TODO: framework code.
            DbObj signedObj = App.instance().getMusubi().objForId(objId);
            getFromNetworkHandlers().handleObj(mContext, DbObjects.forType(type), signedObj);

            // Per-object handlers:
            if (objHandler instanceof FeedMessageHandler) {
                ((FeedMessageHandler) objHandler).handleFeedMessage(mContext, signedObj);
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
            mFromNetworkHandlers.addHandler(new ProfileScanningObjHandler());
            mFromNetworkHandlers.addHandler(new NotificationObjHandler(mHelper));
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
        ProfileScanningObjHandler profileScanningObjHandler = new ProfileScanningObjHandler();
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Set<Long> notSendingObjects = new HashSet<Long>();
        if (DBG) Log.i(TAG, "Running...");
        mMessenger.init();
        long max_sent = -1;
        while (!interrupted()) {
            mOco.waitForChange();
            mOco.clearChanged();
            Cursor objs = mHelper.queryUnsentObjects(max_sent);
            try {
                Log.i(TAG, "Sending " + objs.getCount() + " objects...");
                if(objs.moveToFirst()) do {
                    Long objId = objs.getLong(objs.getColumnIndexOrThrow(DbObject._ID));
                    String jsonSrc = objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON));
                    byte[] raw = objs.getBlob(objs.getColumnIndexOrThrow(DbObject.RAW));
                    Integer intKey = null;
                    if (!objs.isNull(objs.getColumnIndexOrThrow(DbObj.COL_KEY_INT))) {
                        intKey = objs.getInt(objs.getColumnIndexOrThrow(DbObj.COL_KEY_INT));
                    }

                    max_sent = objId.longValue();
                    JSONObject json = null;
                    if (jsonSrc != null) {
                        try {
                            json = new JSONObject(jsonSrc);
                        } catch (JSONException e) {
                            Log.e(TAG, "bad json", e);
                        }
                    } else {
                        json = new JSONObject();
                    }

                    /**
                     * TODO: Hacks in anticipation of new binary-friendly wire format.
                     * This method will need some work!
                     */
                    if (json != null) {
                        if (raw != null) {
                            String type = objs.getString(objs.getColumnIndexOrThrow(DbObject.TYPE));
                            DbEntryHandler e = DbObjects.forType(type);
                            json = e.mergeRaw(json, raw);
                        }
                        if (intKey != null) {
                            json.put(JSON_INT_KEY, intKey);
                        }
                    }

                    if (json != null) {
                        /*if you update latest feed here then there is a race condition between
                         * when you put a message into your db,
                         * when you actually have a connection to send the message (which is here)
                         * when other people send you messages
                         * the processing gets all out of order, so instead we update latest
                         * immediately when you add messages into your db inside
                         * DBHelper.java addToFeed();
                         */
                        //mFeedModifiedObjHandler.handleObj(mContext, feedUri, objId);

                        // TODO: Don't be fooled! This is not truly an EncodedObj
                        // and does not yet have a hash.
                        DbObj signedObj = App.instance().getMusubi().objForId(objId);
                        DbEntryHandler h = DbObjects.getObjHandler(json);
                        if (h != null && h instanceof FeedMessageHandler) {
                            ((FeedMessageHandler) h).handleFeedMessage(mContext, signedObj);
                        }
                        // TODO: Constraint error thrown for now b/c local user not in contacts
                        profileScanningObjHandler.handleObj(mContext, h, signedObj);
                    }
                    String to = objs.getString(objs.getColumnIndexOrThrow(DbObject.DESTINATION));
                    if (DBG) Log.d(TAG, "Sending to: " + to);
                    if (to != null) {
                        OutgoingMessage m = new OutgoingDirectObjectMsg(objs, json);
                        if (DBG) Log.i(TAG, "Sending direct message " + m);
                        if (m.toPublicKeys().isEmpty()) {
                            Log.w(TAG, "No addressees for direct message " + objId);
                            notSendingObjects.add(objId);
                        } else {
                            mMessenger.sendMessage(m);
                        }
                    } else {
                        OutgoingMessage m = new OutgoingFeedObjectMsg(objs, json);
                        if (DBG) Log.i(TAG, "Sending feed object " + objId + ": " + m);
                        if(m.toPublicKeys().isEmpty()) {
                            Log.i(TAG, "No addresses for feed message " + objId);
                            notSendingObjects.add(objId);
                        } else {
                            mMessenger.sendMessage(m);
                        }
                    }
                } while(objs.moveToNext());
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
    	protected SoftReference<byte[]> mEncoded;
        protected String mBody;
        protected List<RSAPublicKey> mPubKeys;
        protected long mObjectId;
        protected JSONObject mJson;
        protected byte[] mRaw;
        protected boolean mDeleteOnCommit;
        protected OutgoingMsg(Cursor objs) {
        	mObjectId = objs.getLong(0 /*DbObject._ID*/);
            DbEntryHandler objHandler = DbObjects.forType(objs.getString(2));
            mDeleteOnCommit = objHandler.discardOutboundObj();
        }
		@Override
		public long getLocalUniqueId() {
			return mObjectId;
		}
        public List<RSAPublicKey> toPublicKeys(){ return mPubKeys; }
        public String contents(){ return mBody; }
        public String toString(){ return "[Message with body: " + mBody + " to " + toPublicKeys().size() + " recipient(s) ]"; }
        public void onCommitted() {
        	mEncoded.clear();
        	mHelper.getWritableDatabase().beginTransaction();
        	try {
	            mHelper.markObjectAsSent(mObjectId);
	            mHelper.clearEncoded(mObjectId);
	            if(mDeleteOnCommit)
	            	mHelper.deleteObj(mObjectId);
	            mHelper.getWritableDatabase().setTransactionSuccessful();
        	} finally {
        		mHelper.getWritableDatabase().endTransaction();
        	}
        }

		@Override
		public void onEncoded(byte[] encoded) {
			mEncoded = new SoftReference<byte[]>(encoded);
			long hash = -1;
			try {
				hash = MessageFormat.extractHash(encoded);
			} catch(CryptoException e) {}
			mHelper.markEncoded(mObjectId, encoded, mJson.toString(), mRaw, hash);
			mJson = null;
			mRaw = null;
			mBody = null;
		}

		@Override
		public byte[] getEncoded() {
			byte[] cached = mEncoded != null ? mEncoded.get() : null;
			if(cached != null)
				return cached;
			cached = mHelper.getEncoded(mObjectId);
			mEncoded = new SoftReference<byte[]>(cached);
			return cached;
		}
		void processRawData() {
            DbEntryHandler h = DbObjects.getObjHandler(mJson);
            if (h != null && h instanceof OutgoingMessageHandler) {
            	Pair<JSONObject, byte[]> r =((OutgoingMessageHandler)h).handleOutgoing(mJson);
            	if(r != null) {
            		mJson = r.first;
            		mRaw = r.second;
            	}
            }
		}
    }

    private class OutgoingFeedObjectMsg extends OutgoingMsg {
        public OutgoingFeedObjectMsg(Cursor objs, JSONObject json){
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
            subs.close();

            mPubKeys = mIdent.publicKeysForContactIds(ids);
            //this obj is not yet encoded
            if(objs.getInt(1) == 0) {
				mJson = json;
	            // the processing code manipulates the json so this has to come first
	            mBody = globalize(mJson.toString());
	            processRawData();
        	}
        }
    }

    private class OutgoingDirectObjectMsg extends OutgoingMsg {
        public OutgoingDirectObjectMsg(Cursor objs, JSONObject json) {
            super(objs);
            String to = objs.getString(objs.getColumnIndexOrThrow(DbObject.DESTINATION));
            try {
                List<Long> ids = Util.splitLongsToList(to, ",");
                mPubKeys = mIdent.publicKeysForContactIds(ids);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Bad destination found: '" + to + "'");
                mPubKeys = new ArrayList<RSAPublicKey>();
            }
            //this obj is not yet encoded
            if(objs.getInt(1) == 0) {
				mJson = json;
	            mBody = globalize(mJson.toString());
	            // the processing code manipulates the json so this has to come first
	            processRawData();
            }
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
