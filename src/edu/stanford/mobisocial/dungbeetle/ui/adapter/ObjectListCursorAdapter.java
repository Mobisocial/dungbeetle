package edu.stanford.mobisocial.dungbeetle.ui.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Group;

public class ObjectListCursorAdapter extends CursorAdapter {
	private Cursor originalCursor;
	private int mTotal = BATCH_SIZE;

    public ObjectListCursorAdapter (Context context, Cursor cursor) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        // TODO: does contact cache handle images and attributes?
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.objects_item, parent, false);
        bindView(v, context, c);

        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {
        DbObject.bindView(v, context, c, true);
        
    }
    
    static final int BATCH_SIZE = getBestBatchSize();
    
    public int getTotalQueried() {
    	return mTotal;
    }

    public static CursorLoader queryObjects(Context context, Uri feedUri, String[] types) {
        return new CursorLoader(context, feedUri, 
        	new String[] { 
        		DbObject._ID,
        		DbObject.FEED_NAME
        	},
        	DbObjects.getFeedObjectClause(types), null, DbObject._ID + 
        	" DESC LIMIT " + BATCH_SIZE);
    }
    private static int getBestBatchSize() {
    	Runtime runtime = Runtime.getRuntime();
    	if(runtime.availableProcessors() > 1)
    		return 100;

    	try {
			File max_cpu_freq = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
			byte[] freq_bytes = IOUtils.toByteArray(new FileInputStream(max_cpu_freq));
			String freq_string = new String(freq_bytes);
			double freq = Double.valueOf(freq_string);
			if(freq > 950000) {
				return 50;
			}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
		return 15;
	}

	public CursorLoader queryLaterObjects(Context context, Uri feedUri, int total, String[] types) {
    	mTotal = total + BATCH_SIZE;
    	CursorLoader cl = new CursorLoader(context, feedUri, 
            	new String[] { 
            		DbObject._ID,
            		DbObject.FEED_NAME
            	},
            	DbObjects.getFeedObjectClause(types), null, DbObject._ID + " DESC LIMIT " + mTotal);
		Cursor newCursor = cl.loadInBackground(); 
		
    	if (originalCursor == null) {
    		originalCursor = this.swapCursor(newCursor);
    	}	
    	else {
    		this.changeCursor(newCursor);
    	}
    	return cl;
    }
    
    public void closeCursor() {
    	if (originalCursor != null) {
    		originalCursor.close();
    	}
    }
}
