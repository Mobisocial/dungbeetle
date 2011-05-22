package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.ImageViewerActivity;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;


import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView;

import android.util.Base64;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import edu.stanford.mobisocial.dungbeetle.R;

public class VoiceObj implements FeedRenderer, Activator {
	public static final String TAG = "VoiceObj";

    public static final String TYPE = "voice";
    public static final String DATA = "data";

    
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        
    public static JSONObject json(byte[] data){
        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        JSONObject obj = new JSONObject();
        try{
            obj.put("data", encoded);
        }catch(JSONException e){}
        return obj;
    }

	public boolean willRender(JSONObject object) { 
        return object.optString("type").equals(TYPE);
	}
	
	public void render(Context context, ViewGroup frame, JSONObject content) {
		ImageView imageView = new ImageView(context);
		imageView.setImageResource(R.drawable.play);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.WRAP_CONTENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(imageView);
	}

    public void activate(Context context, JSONObject content){
        byte bytes[] = Base64.decode(content.optString(DATA), Base64.DEFAULT);
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bytes.length, AudioTrack.MODE_STATIC);
        track.write(bytes, 0, bytes.length);
        track.play();
    }

	public boolean willActivate(JSONObject object) { 
        return object.optString("type").equals(TYPE);
	}

}
