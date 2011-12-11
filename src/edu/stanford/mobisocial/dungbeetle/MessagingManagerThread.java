
package edu.stanford.mobisocial.dungbeetle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mobisocial.socialkit.EncodedObj;
import mobisocial.socialkit.PreparedObj;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.User;
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
import edu.stanford.mobisocial.dungbeetle.util.Util;

public class MessagingManagerThread extends Thread {
    public static final String TAG = "MessagingManagerThread";
    public static final boolean DBG = true;
    private Context mContext;
    private MessengerService mMessenger;
    private ObjectContentObserver mOco;
    private DBHelper mHelper;
    private IdentityProvider mIdent;
    private final MessageDropHandler mMessageDropHandler;
    private static final String JSON_INT_KEY = "obj_intkey";

    public MessagingManagerThread(final Context context) {
        mContext = context;
        mHelper = DBHelper.getGlobal(context);
        mIdent = new DBIdentityProvider(mHelper);
        mMessageDropHandler = new MessageDropHandler();

        ConnectionStatus status = new ConnectionStatus() {
            public boolean isConnected() {
                ConnectivityManager cm = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
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
                if (DBG)
                    Log.i(TAG, "Got incoming message " + incoming);
                handleIncomingMessage(incoming);
            }
        });
        mMessenger.addConnectionStatusListener(new ConnectionStatusListener() {

            @Override
            public void onStatus(String msg, Exception e) {
                StringWriter err = new StringWriter();
                PrintWriter p = new PrintWriter(err);
                if (e != null) {
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
    private void handleIncomingMessage(final IncomingMessage incoming) {
        final SignedObj contents = incoming.contents();
        final long hash = contents.getHash();
        if (contents.getSender() == null) {
            Log.e(TAG, "Null sender for " + contents.getType() + ", " + contents.getJson());
            return;
        }
        final String personId = contents.getSender().getId();
        // final String personId = incoming.from();

        /**
         * TODO: This needs to be updated with the POSI standards to accept a
         * SignedObj.
         */

        if (DBG)
            Log.i(TAG, "Localized contents: " + contents);
        try {
            JSONObject in_obj = contents.getJson();
            String feedName = contents.getFeedName();
            String type = contents.getType();
            Uri feedPreUri = Feed.uriForName(feedName);
            if (mMessageDropHandler.preFiltersObj(mContext, feedPreUri)) {
                return;
            }

            if (mHelper.queryAlreadyReceived(hash)) {
                if (DBG)
                    Log.i(TAG, "Message already received: " + hash);
                return;
            }

            Maybe<Contact> contact = mHelper.contactForPersonId(personId);
            final DbEntryHandler objHandler = DbObjects.getObjHandler(in_obj);
            byte[] extracted_data = null;
            if (objHandler instanceof UnprocessedMessageHandler) {
                Pair<JSONObject, byte[]> r = ((UnprocessedMessageHandler) objHandler)
                        .handleUnprocessed(mContext, in_obj);
                if (r != null) {
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

            /**
             * Run handlers over all received objects:
             */

            long objId;
            final Contact realContact = contact.get();
            long contactId = realContact.id;
            if (DBG)
                Log.d(TAG, "Msg from " + contactId + " ( " + realContact.name + ")");
            // Insert into the database. (TODO: Handler, both android.os and
            // musubi.core)

            if (!objHandler.handleObjFromNetwork(mContext, realContact, obj)) {
                return;
            }

            Integer intKey = null;
            if (obj.has(JSON_INT_KEY)) {
                intKey = obj.getInt(JSON_INT_KEY);
                obj.remove(JSON_INT_KEY);
            }
            objId = mHelper.addObjectByJson(contact.otherwise(Contact.NA()).id, obj, hash, raw,
                    intKey);
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
            objHandler.afterDbInsertion(mContext, signedObj);
        } catch (Exception e) {
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

    @Override
    public void run() {
        ProfileScanningObjHandler profileScanningObjHandler = new ProfileScanningObjHandler();
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Set<Long> notSendingObjects = new HashSet<Long>();
        if (DBG)
            Log.i(TAG, "Running...");
        mMessenger.init();
        long max_sent = -1;
        while (!interrupted()) {
            mOco.waitForChange();
            mOco.clearChanged();
            Cursor objs = mHelper.queryUnsentObjects(max_sent);
            try {
                Log.i(TAG, "Sending " + objs.getCount() + " objects...");
                if (objs.moveToFirst())
                    do {
                        Long objId = objs.getLong(objs.getColumnIndexOrThrow(DbObject._ID));
                        String jsonSrc = objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON));

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

                        if (json != null) {
                            /*
                             * if you update latest feed here then there is a
                             * race condition between when you put a message
                             * into your db, when you actually have a connection
                             * to send the message (which is here) when other
                             * people send you messages the processing gets all
                             * out of order, so instead we update latest
                             * immediately when you add messages into your db
                             * inside DBHelper.java addToFeed();
                             */
                            // mFeedModifiedObjHandler.handleObj(mContext,
                            // feedUri, objId);

                            // TODO: Don't be fooled! This is not truly an
                            // EncodedObj
                            // and does not yet have a hash.
                            DbObj signedObj = App.instance().getMusubi().objForId(objId);
                            if (signedObj == null) {
                                Log.e(TAG, "Error, object " + objId + " not found in database");
                                notSendingObjects.add(objId);
                                continue;
                            }
                            DbEntryHandler h = DbObjects.getObjHandler(json);
                            h.afterDbInsertion(mContext, signedObj);

                            // TODO: Constraint error thrown for now b/c local
                            // user not in contacts
                            profileScanningObjHandler.handleObj(mContext, h, signedObj);
                        }

                        OutgoingMessage m = new OutgoingMsg(objs);
                        if (m.contents().getRecipients().isEmpty()) {
                            Log.w(TAG, "No addressees for direct message " + objId);
                            notSendingObjects.add(objId);
                        } else {
                            mMessenger.sendMessage(m);
                        }
                    } while (objs.moveToNext());
                if (notSendingObjects.size() > 0) {
                    if (DBG)
                        Log.d(TAG, "Marking " + notSendingObjects.size() + " objects sent");
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

    private List<Long> getFeedSubscribers(Uri feedUri) {
        if (feedUri == null) {
            throw new NullPointerException("Feed cannot be null");
        }
        String feedName = feedUri.getLastPathSegment();
        switch (Feed.typeOf(feedUri)) {
            case FRIEND:
                String personId = Feed.personIdForFeed(feedUri);
                if (personId == null) {
                    return new ArrayList<Long>(0);
                }
                String table = Contact.TABLE;
                String[] columns = new String[] { Contact._ID };
                String selection = Contact.PERSON_ID + " = ?";
                String[] selectionArgs = new String[] { personId };
                String groupBy = null;
                String having = null;
                String orderBy = null;
                Cursor c = mHelper.getReadableDatabase().query(
                        table, columns, selection, selectionArgs, groupBy, having, orderBy);
                if (c == null || !c.moveToFirst()) {
                    Log.w(TAG, "Could not find user for id " + personId);
                    return new ArrayList<Long>(0);
                }
                return Collections.singletonList(c.getLong(0));
            case APP:
                // Currently, we send app messages to all users, which are registered
                // as subscribers to the "friend" feed. The subscribers model needs to
                // be reworked, and further the "app" feed needs further thinking.
                // Messages should be lossy, and encryption should not require keys
                // for each recipient.
                feedName = "friend";
                // No break:
            case GROUP:
                Cursor subs = mHelper.querySubscribers(feedName);
                List<Long> recipientIds = new ArrayList<Long>(subs.getCount());
                subs.moveToFirst();
                while (!subs.isAfterLast()) {
                    long id = subs.getLong(subs.getColumnIndexOrThrow(Subscriber.CONTACT_ID));
                    recipientIds.add(id);
                    subs.moveToNext();
                }
                subs.close();
                return recipientIds;
            default:
                Log.w(TAG, "unmatched feed type for " + feedUri);
                return new ArrayList<Long>();
        }
    }

    private class OutgoingMsg implements OutgoingMessage {
        protected SoftReference<EncodedObj> mEncoded;
        protected PreparedObj mBody;
        protected long mObjectId;
        protected byte[] mRaw;
        protected boolean mDeleteOnCommit;

        protected OutgoingMsg(Cursor objs) {
            mObjectId = objs.getLong(0 /* DbObject._ID */);
            DbEntryHandler objHandler = DbObjects.forType(objs.getString(2));
            mDeleteOnCommit = objHandler.discardOutboundObj();

            String feedName = objs.getString(objs.getColumnIndexOrThrow(DbObject.FEED_NAME));
            Uri feedUri = Feed.uriForName(feedName);
            String to = objs.getString(
                    objs.getColumnIndexOrThrow(DbObject.DESTINATION));

            if (DBG) Log.d(TAG, "Sending to: " + feedName + ", " + to);
            List<Long> recipientIds;
            if (to != null) {
                recipientIds = Util.splitLongsToList(to, ",");
            } else {
                recipientIds = getFeedSubscribers(feedUri);
            }

            List<User> recipients = mHelper.getPKUsersForIds(recipientIds);
            String type = objs.getString(objs.getColumnIndexOrThrow(DbObject.TYPE));
            String appId = objs.getString(objs.getColumnIndexOrThrow(DbObject.APP_ID));
            String jsonSrc = objs.getString(objs.getColumnIndexOrThrow(DbObject.JSON));
            mRaw = objs.getBlob(objs.getColumnIndexOrThrow(DbObject.RAW));
            Integer intKey = null;
            int col = objs.getColumnIndexOrThrow(DbObject.KEY_INT);
            if (!objs.isNull(col)) {
                intKey = objs.getInt(col);
            }
            JSONObject json = null;
            try {
                json = new JSONObject(jsonSrc);
                User sender = App.instance().getMusubi().userForLocalDevice(feedUri);
                mBody = new DbPreparedObj(sender, recipients, appId, type, json, mRaw, intKey);
            } catch (JSONException e) {
                Log.e(TAG, "Bad json in db", e);
            }
        }

        @Override
        public long getLocalUniqueId() {
            return mObjectId;
        }

        public PreparedObj contents() {
            return mBody;
        }

        public String toString() {
            return "[Message with body: " + mBody + "]";
        }

        public void onCommitted() {
            mEncoded.clear();
            mHelper.getWritableDatabase().beginTransaction();
            try {
                mHelper.markObjectAsSent(mObjectId);
                mHelper.clearEncoded(mObjectId);
                if (mDeleteOnCommit)
                    mHelper.deleteObj(mObjectId);
                mHelper.getWritableDatabase().setTransactionSuccessful();
            } finally {
                mHelper.getWritableDatabase().endTransaction();
            }
        }

        @Override
        public void onEncoded(EncodedObj encoded) {
            mEncoded = new SoftReference<EncodedObj>(encoded);
            Log.d(TAG, "Setting encoded with hash " + encoded.getHash());
            mHelper.setEncoded(mObjectId, encoded);
            mRaw = null;
        }

        @Override
        public EncodedObj getEncoded() {
            EncodedObj cached = mEncoded != null ? mEncoded.get() : null;
            if (cached != null) {
                Log.d(TAG, "fetching memcached encoding " + cached.getHash());
                return cached;
            }
            cached = mHelper.getEncoded(mObjectId);
            mEncoded = new SoftReference<EncodedObj>(cached);
            return cached;
        }
    }

    private TransportIdentityProvider wrapIdent(final IdentityProvider ident) {
        return new TransportIdentityProvider() {
            public RSAPublicKey userPublicKey() {
                return ident.userPublicKey();
            }

            public RSAPrivateKey userPrivateKey() {
                return ident.userPrivateKey();
            }

            public String userPersonId() {
                return ident.userPersonId();
            }

            public RSAPublicKey publicKeyForPersonId(String id) {
                return ident.publicKeyForPersonId(id);
            }

            public String personIdForPublicKey(RSAPublicKey key) {
                return ident.personIdForPublicKey(key);
            }

            @Override
            public User userForPersonId(String id) {
                Uri feedUri = Feed.uriForName(Feed.FEED_NAME_GLOBAL);
                User u = App.instance().getMusubi().userForGlobalId(feedUri, id);
                return u;
            }
        };
    }

    class ObjectContentObserver extends ContentObserver {
        public boolean changed;

        public ObjectContentObserver(Handler h) {
            super(h);
            changed = true;
        }

        @Override
        public synchronized void onChange(boolean self) {
            changed = true;
            notify();
        }

        public synchronized void waitForChange() {
            if (changed)
                return;
            try {
                wait();
                changed = false;
            } catch (InterruptedException e) {
            }
        }

        public synchronized void clearChanged() {
            changed = false;
        }
    };

    static class DbPreparedObj implements PreparedObj {
        final String mType;
        final String mAppId;
        final String mFeedName;
        final JSONObject mJson;
        final List<User> mRecipients;
        final User mSender;
        final byte[] mRaw;
        final Integer mIntKey;

        public DbPreparedObj(User sender, List<User> recipients, String appId,
                String type, JSONObject json, byte[] raw, Integer intKey) {
            mFeedName = json.optString(DbObjects.FEED_NAME);
            mRecipients = recipients;
            mAppId = appId;
            mSender = sender;
            mType = type;
            mRaw = raw;
            mIntKey = intKey;
            mJson = json;
        }

        @Override
        public Integer getInt() {
            return mIntKey;
        }

        @Override
        public JSONObject getJson() {
            return mJson;
        }

        @Override
        public byte[] getRaw() {
            return mRaw;
        }

        @Override
        public String getType() {
            return mType;
        }

        @Override
        public String getAppId() {
            return mAppId;
        }

        @Override
        public User getSender() {
            return mSender;
        }

        @Override
        public List<User> getRecipients() {
            return mRecipients;
        }

        @Override
        public long getSequenceNumber() {
            try {
                return mJson.getLong(DbObjects.SEQUENCE_ID);
            } catch (Exception e) {
                return -1;
            }
        }

        @Override
        public String getFeedName() {
            return mFeedName;
        }

        @Override
        public long getTimestamp() {
            try {
                return mJson.getLong(DbObjects.TIMESTAMP);
            } catch (Exception e) {
                return -1;
            }
        }
    }
}
