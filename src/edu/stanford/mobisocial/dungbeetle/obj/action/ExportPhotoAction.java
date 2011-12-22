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


package edu.stanford.mobisocial.dungbeetle.obj.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

/**
 * Sends a picture object using the standard Android "SEND" intent.
 *
 */
public class ExportPhotoAction extends ObjAction {
    @Override
    public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        byte[] raw = obj.getRaw();
        if (raw == null) {
            String b64Bytes = obj.getJson().optString(PictureObj.DATA);
        	raw = FastBase64.decode(b64Bytes);
        }
        OutputStream outStream = null;
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_share.png");
        try {
            outStream = new FileOutputStream(file);
            
	        BitmapManager mgr = new BitmapManager(1);
	        Bitmap bitmap = mgr.getBitmap(raw.hashCode(), raw);
	        
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            bitmap.recycle();
            bitmap = null;
            System.gc();
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);  
            intent.setType("image/png");
            Log.w("ResharePhotoAction", Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_share.png");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file)); 
            context.startActivity(Intent.createChooser(intent, "Export image to"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public String getLabel(Context context) {
        return "Export";
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        /*if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }*/
        return (objType instanceof PictureObj);
    }
}
