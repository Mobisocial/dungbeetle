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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import mobisocial.socialkit.musubi.DbObj;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;

/**
 * Plays all audio clips that are at least as recent than the one
 * being clicked.
 *
 */
public class PlayAllAudioAction extends ObjAction {

    private static final int RECORDER_BPP = 16;

	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AlertDialog alert;
	private Cursor c;
	private MediaPlayer mp;
	private Context context;

/* example query

SELECT * FROM objects WHERE feed_name='0e5f3fd1-c3d4-4411-b54c-18260bdfa24e' AND type='voice' AND _id >= (
SELECT _id
FROM objects 
WHERE feed_name='0e5f3fd1-c3d4-4411-b54c-18260bdfa24e' AND type='voice' AND sequence_id='17' AND timestamp='1315348974072')
ORDER BY _id ASC
*/


    	private String getTempFilename(){
            return Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.raw";
        }

        private String getFilename(){
            return Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.wav";
        }

        public void playNextSong() {
            if (c.isAfterLast()) {
                c.close();
                alert.dismiss();
                return;
            }

            try {
                final JSONObject objData = new JSONObject(c.getString(c.getColumnIndex(DbObject.JSON)));
                Runnable r = new Runnable() {
	                @Override
	                public void run() {
	                
	                    Log.w("PlayAllAudioAction", objData.optString("feedName"));
	                    byte bytes[] = Base64.decode(objData.optString(VoiceObj.DATA), Base64.DEFAULT);

                        File file = new File(getTempFilename());
                        try {
                            OutputStream os = new FileOutputStream(file);
                            BufferedOutputStream bos = new BufferedOutputStream(os);
                            bos.write(bytes, 0, bytes.length);
                            bos.flush();
                            bos.close();

                        copyWaveFile(getTempFilename(),getFilename());
                        deleteTempFile();

                               
                            mp = new MediaPlayer();

                            mp.setDataSource(getFilename());
                            mp.setOnCompletionListener(new OnCompletionListener() {
                                
                                public void onCompletion(MediaPlayer m) {
                                    Log.w("PlayAllAudioAction", "finished");
                                    c.moveToNext();
                                    playNextSong();
                                }
                            });
                            mp.prepare();
                            mp.start();
                        } catch (Exception e) {
                        // TODO Auto-generated catch block
                            e.printStackTrace();
                        } 
	                }
	            };
                if (context instanceof Activity) {
                    ((Activity)context).runOnUiThread(r);
                } else {
                    r.run();
                }
            }
            catch(Exception e) {
            }
            
        }
	
        public void onAct(Context context, DbEntryHandler objType, DbObj obj) {
        DBHelper helper = DBHelper.getGlobal(context);
        this.context = context;
        JSONObject objData = obj.getJson();
        
        //TODO: this cursor really need to be closed somewhere!!!  it may be but its sketchy
        //TODO: holy frickin hell its sketch... slutty code, plus it pulls the full body in as well
        //it should maybe load an objid list and then fetch each voice obj individually.

        String query = DbObject.FEED_NAME + "= ? AND "+DbObject.TYPE+"='voice' AND " + DbObject._ID +
                " >= (SELECT " + DbObject._ID + " FROM " + DbObject.TABLE + " WHERE " +
                DbObject.FEED_NAME + "= ? AND " + DbObject.TYPE + "='voice' AND " +
                DbObject.SEQUENCE_ID + "= ? AND " + DbObject.TIMESTAMP+"= ?)";
        String[] queryParams = new String[]{objData.optString("feedName"), objData.optString("feedName"), objData.optString("sequenceId"), objData.optString("timestamp")};
        c = helper.getReadableDatabase().query(DbObject.TABLE, null, query, queryParams,
                null, null, DbObject._ID + " ASC");
        
        c.moveToFirst();

        /*while(!c.isAfterLast()) {
            Log.w("PlayAllAudioAction", c.getString(c.getColumnIndex(DbObject.JSON)));
            c.moveToNext();
        }*/
        playNextSong();
        //c.close();
        
        Log.w("PlayAllAudioAction", c.getCount() + " rows");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Now Playing Voice Messages")
               .setCancelable(false)
               .setNegativeButton("Stop Playing", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        c.moveToLast();
                        c.moveToNext();
                        mp.stop();
                        mp.release();
                   }
               });
        alert = builder.create();
        alert.show();
        
        
    }

            private void deleteTempFile() {
                File file = new File(getTempFilename());
                
                file.delete();
        }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        byte[] data = new byte[bufferSize];
        
        try {
                in = new FileInputStream(inFilename);
                out = new FileOutputStream(outFilename);
                totalAudioLen = in.getChannel().size();
                totalDataLen = totalAudioLen + 36;
                
                Log.w("PlayAllAudioAction", "File size: " + totalDataLen);
                
                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                                longSampleRate, channels, byteRate);
                
                while(in.read(data) != -1){
                        out.write(data);
                }
                
                in.close();
                out.close();
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
                    FileOutputStream out, long totalAudioLen,
                    long totalDataLen, long longSampleRate, int channels,
                    long byteRate) throws IOException {
                
        byte[] header = new byte[44];
        
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }


    @Override
    public String getLabel(Context context) {
        return "Replay Conversation";
    }

    @Override
    public boolean isActive(Context context, DbEntryHandler objType, JSONObject objData) {
        if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }
        return (objType instanceof VoiceObj);
    }
}
