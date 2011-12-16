/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class PresenceThread extends Thread {
    public static final String TAG = "PresenceThread";
    private Context mContext;
    private DBHelper mHelper;
    private IdentityProvider mIdent;

    public PresenceThread(final Context context){
        mContext = context;
        mHelper = DBHelper.getGlobal(context);
        mIdent = new DBIdentityProvider(mHelper);
    }

    @Override
    public void run(){
        Log.i(TAG, "Running...");
        while(!interrupted()) {
            try{
                // Cursor c = mHelper.sampleFromContacts(10);
                // c.moveToFirst();
                // while(!c.isAfterLast()){
                    
                //     c.moveToNext();

                // }
                if(App.instance().isScreenOn()){
                    Thread.sleep(10000);
                }
                else{
                    Thread.sleep(60000);
                }
            }
            catch(Exception e){
                Log.wtf(TAG, e);
            }
        }
        mHelper.close();
    }


}
