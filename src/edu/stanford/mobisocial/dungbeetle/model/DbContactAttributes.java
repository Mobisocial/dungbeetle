package edu.stanford.mobisocial.dungbeetle.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;

public class DbContactAttributes /* extends DbTable */ {
    public static final String TABLE = "contact_attributes";
    public static final String _ID = "_id";
    public static final String CONTACT_ID = "contact_id";
    public static final String ATTR_NAME = "attr_name";
    public static final String ATTR_VALUE = "attr_value";

    public static final String[] getColumnNames() {
        return new String[] { _ID, CONTACT_ID, ATTR_NAME, ATTR_VALUE };
    }

    public static final String[] getTypeDefs() {
        return new String[] { "INTEGER PRIMARY KEY", "INTEGER", "TEXT", "TEXT" };
    }

    public static void update(Context context, Contact contact, String attr, String value) {
        ContentValues values = new ContentValues();
        values.put(CONTACT_ID, contact.id);
        values.put(ATTR_NAME, attr);
        values.put(ATTR_VALUE, value);

        DBHelper helper = new DBHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] columns = { _ID };
            String selection = CONTACT_ID + " = ? AND " + ATTR_NAME + " = ?";
            String[] selectionArgs = new String[] { Long.toString(contact.id), attr };
            String groupBy = null;
            String having = null;
            String orderBy = null;
            Cursor c = db.query(TABLE, columns, selection, selectionArgs, groupBy, having, orderBy);
            if (c.moveToFirst()) {
                db.update(TABLE, values, selection, selectionArgs);
            } else {
                db.insert(TABLE, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
        helper.close();
    }
}