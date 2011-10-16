package edu.stanford.mobisocial.dungbeetle.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    public static void update(Context context, long contactId, String attr, String value) {
        ContentValues values = new ContentValues();
        values.put(CONTACT_ID, contactId);
        values.put(ATTR_NAME, attr);
        values.put(ATTR_VALUE, value);

        DBHelper helper = new DBHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] columns = { _ID };
            String selection = CONTACT_ID + " = ? AND " + ATTR_NAME + " = ?";
            String[] selectionArgs = new String[] { Long.toString(contactId), attr };
            String groupBy = null;
            String having = null;
            String orderBy = null;
            Cursor c = db.query(TABLE, columns, selection, selectionArgs, groupBy, having, orderBy);
            if (c.moveToFirst()) {
                c.close();
                db.update(TABLE, values, selection, selectionArgs);
            } else {
                c.close();
                db.insert(TABLE, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        helper.close();
    }

    public static String getAttribute(Context context, long contactId, String attr) {
        DBHelper helper = new DBHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        String[] columns = { ATTR_VALUE };
        String selection = CONTACT_ID + " = ? AND " + ATTR_NAME + " = ?";
        String[] selectionArgs = new String[] { Long.toString(contactId), attr };
        String groupBy = null;
        String having = null;
        String orderBy = null;
        Cursor c = db.query(TABLE, columns, selection, selectionArgs, groupBy, having, orderBy);
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        } finally {
            c.close();
            helper.close();
        }
    }
}