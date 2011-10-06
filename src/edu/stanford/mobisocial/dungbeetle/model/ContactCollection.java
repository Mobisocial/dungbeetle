package edu.stanford.mobisocial.dungbeetle.model;
import android.database.Cursor;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Lazy iterator of contacts in a group.
 */
public class ContactCollection extends AbstractCollection<Contact> {
	//This used to lazy load through an iterator, but the problem with
	//that is that it isn't possible to know when to clean up
	//the cursor!
	LinkedList<Contact> contacts_ = new LinkedList<Contact>();

    public ContactCollection(long groupId, DBHelper helper){
        Cursor cursor = helper.queryGroupContacts(groupId);
        try {
	        if(cursor.moveToFirst()) do {
	        	contacts_.add(new Contact(cursor));
	        } while(cursor.moveToNext());
        } finally {
        	cursor.close();
        }
    }

    @Override
    public int size(){
        return contacts_.size();
    }

    @Override
    public Iterator<Contact> iterator(){
    	return contacts_.iterator();
    }
}
