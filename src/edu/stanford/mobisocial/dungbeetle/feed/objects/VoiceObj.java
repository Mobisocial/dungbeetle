package edu.stanford.mobisocial.dungbeetle.feed.objects;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.feed.iface.OutgoingMessageHandler;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

/**
 * A short audio clip. "Version 0" uses a sample rate of 8000, mono channel, and
 * 16bit pcm recording.
 */
public class VoiceObj extends DbEntryHandler implements FeedRenderer, Activator, OutgoingMessageHandler {
	public static final String TAG = "VoiceObj";

    public static final String TYPE = "voice";
    public static final String DATA = "data";


    private static final int RECORDER_BPP = 16;
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    @Override
    public String getType() {
        return TYPE;
    }
	public JSONObject mergeRaw(JSONObject objData, byte[] raw) {
		try {
			if(raw != null)
				objData = objData.put(DATA, Base64.encodeToString(raw, Base64.DEFAULT));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return objData;
	}
	@Override
	public Pair<JSONObject, byte[]> splitRaw(JSONObject json) {
		byte[] raw = Base64.decode(json.optString(DATA), Base64.DEFAULT);
		json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, raw);
	}

    public static DbObject from(byte[] data) {
        return new DbObject(TYPE, json(data));
    }
    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
        }catch(JSONException e){}
        return obj;
    }
	
	public void render(Context context, ViewGroup frame, JSONObject content, byte[] raw, boolean allowInteractions) {
		ImageView imageView = new ImageView(context);
		imageView.setImageResource(R.drawable.play);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(imageView);
	}

	private String getTempFilename(){
        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.raw";
    }

    private String getFilename(){
        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.wav";
    }
	
	@Override
    public void activate(final Context context, long contactId, final JSONObject content, final byte[] raw) {
	    Runnable r = new Runnable() {
	        @Override
	        public void run() {
	        	byte[] bytes = raw;
	    		if(bytes == null) {
	    			Pair<JSONObject, byte[]> p = splitRaw(content);
//	    			content = p.first;
	    			bytes = p.second;
	    		}

	            /*AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bytes.length, AudioTrack.MODE_STATIC);
	            track.write(bytes, 0, bytes.length);
	            try { // TODO: hack.
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
	            track.play();
	            */
	            /****/

                File file = new File(getTempFilename());
                try {
                    OutputStream os = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(os);
                    bos.write(bytes, 0, bytes.length);
                    bos.flush();
                    bos.close();

                    copyWaveFile(getTempFilename(),getFilename());
                    deleteTempFile();

                       
                    MediaPlayer mp = new MediaPlayer();

                    mp.setDataSource(getFilename());
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
	public Pair<JSONObject, byte[]> handleOutgoing(JSONObject json) {
        byte[] bytes = Base64.decode(json.optString(DATA), Base64.DEFAULT);
        json.remove(DATA);
		return new Pair<JSONObject, byte[]>(json, bytes);
	}
	
    @Override
    public void handleDirectMessage(Context context, Contact from, JSONObject msg) {
        // TODO Auto-generated method stub
        
    }

}
