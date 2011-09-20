
package edu.stanford.mobisocial.dungbeetle.util;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

public class ContactCache extends ContentObserver {
    private final Context mContext;
    private final DBHelper mHelper;
    private final DBIdentityProvider mIdent;

    public ContactCache(Context context) {
        super(new Handler(context.getMainLooper()));
        mContext = context;
        mHelper = new DBHelper(context); 
        mIdent = new DBIdentityProvider(mHelper);
        context.getContentResolver().registerContentObserver(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), true, this);

        // So we pick up changes to user's profile image..
        context.getContentResolver().registerContentObserver(
                Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/my_info"), true, this);
    }

    private Map<Long, Contact> mContactCache = new HashMap<Long, Contact>();

    @Override
    public void onChange(boolean self) {
        mContactCache.clear();
    }

    public Maybe<Contact> getContact(long id) {
        if (mContactCache.containsKey(id)) {
            return Maybe.definitely(mContactCache.get(id));
        } else {
            if (id == Contact.MY_ID) {
                Contact contact = mIdent.contactForUser();
                mContactCache.put(id, contact);
                return Maybe.definitely(contact);
            } else {
                Cursor c = mContext.getContentResolver().query(
                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts"), null,
                        Contact._ID + "=?", new String[] {
                            String.valueOf(id)
                        }, null);
                try {
	                c.moveToFirst();
	                if (c.isAfterLast()) {
	                    return Maybe.unknown();
	                } else {
	                    Contact contact = new Contact(c);
	                    mContactCache.put(id, contact);
	                    return Maybe.definitely(contact);
	                }
                } finally {
                	c.close();
                }
            }
        }
    }

    public void close() {
        mIdent.close();
    }
}
