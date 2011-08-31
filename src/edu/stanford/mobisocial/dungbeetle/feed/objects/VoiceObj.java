package edu.stanford.mobisocial.dungbeetle.feed.objects;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;


import edu.stanford.mobisocial.dungbeetle.feed.iface.Activator;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedRenderer;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;

import android.util.Base64;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;



import edu.stanford.mobisocial.dungbeetle.R;

public class VoiceObj implements DbEntryHandler, FeedRenderer, Activator {
	public static final String TAG = "VoiceObj";

    public static final String TYPE = "voice";
    public static final String DATA = "data";

    
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    @Override
    public String getType() {
        return TYPE;
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
	
	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
		imageView.setImageResource(R.drawable.play);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(imageView);
	}

	@Override
    public void activate(Context context, JSONObject content){
        byte bytes[] = Base64.decode(content.optString(DATA), Base64.DEFAULT);
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bytes.length, AudioTrack.MODE_STATIC);
        track.write(bytes, 0, bytes.length);
        track.play();
    }


    @Override
    public void handleReceived(Context context, Contact from, JSONObject msg) {
        // TODO Auto-generated method stub
        
    }

}
