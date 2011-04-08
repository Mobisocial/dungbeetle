package edu.stanford.mobisocial.dungbeetle.model;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * Lazy iterator of contacts in a group.
 */
public class ContactCollection extends AbstractCollection<Contact> {
    public final Cursor mCursor;

    public ContactCollection(long groupId, DBHelper helper){
        mCursor = helper.queryGroupContacts(groupId);
    }

    @Override
    public int size(){
        return mCursor.getCount();
    }

    @Override
    public Iterator<Contact> iterator(){
        mCursor.moveToFirst();
        return new Iterator<Contact>(){
            @Override
            public Contact next(){
                Contact c = new Contact(mCursor);
                mCursor.moveToNext();
                return c;
            }
            @Override
            public boolean hasNext(){
                return !mCursor.isAfterLast();
            }
            @Override
            public void remove(){
                throw new UnsupportedOperationException();
            }
        };
    }
}
