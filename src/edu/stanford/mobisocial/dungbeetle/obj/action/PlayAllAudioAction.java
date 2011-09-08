
package edu.stanford.mobisocial.dungbeetle.obj.action;

import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.DbEntryHandler;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import android.util.Base64;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.app.Activity;
import android.util.Log;

public class PlayAllAudioAction extends ObjAction {


	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	int frameStop = 5;
    public void onAct(Context context, DbEntryHandler objType, final JSONObject objData) {
        Runnable r = new Runnable() {
	        @Override
	        public void run() {
	            byte bytes[] = Base64.decode(objData.optString(VoiceObj.DATA), Base64.DEFAULT);
	            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bytes.length, AudioTrack.MODE_STATIC);
	            track.write(bytes, 0, bytes.length);
	            try { // TODO: hack.
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                int audioLength = (int) bytes.length/2;
                Log.w("playAllAudio", "audiolength=" + audioLength);
                track.setNotificationMarkerPosition(audioLength);
                track.setPlaybackPositionUpdateListener(
			        new AudioTrack.OnPlaybackPositionUpdateListener() {
				        static final String TAG = "OnPlaybackPositionUpdateListener";
			            int periodicCount;

			            public void onPeriodicNotification(AudioTrack track) {
			            	++periodicCount;
					        logAudioTrackStatus(track,
						        "onPeriodicNotification " + periodicCount, TAG);
			            }

			            public void onMarkerReached(AudioTrack track) {
			            	Log.w(TAG, "onMarkerReached " + track.getPlaybackHeadPosition());
			            }
			        }
		        );
		        Log.w("playAllAudio", "final frame: " + frameStop);
	            track.play();
	        }
	    };
        if (context instanceof Activity) {
            ((Activity)context).runOnUiThread(r);
        } else {
            r.run();
        }
        
    }

    private void logAudioTrackStatus(AudioTrack audioTrack, String msgPrefix,
	        String tagPrefix) {
		Log.i(tagPrefix + " AudioTrack status", "[" + msgPrefix + "] state: "
		        + audioTrack.getState() + ", playback state: "
		        + audioTrack.getPlayState() + ", current position: "
		        + audioTrack.getPlaybackHeadPosition() + ", period: "
		        + audioTrack.getPositionNotificationPeriod() + ", marker at: "
		        + audioTrack.getNotificationMarkerPosition());
	}

    @Override
    public String getLabel() {
        return "Play conversation from here";
    }

    @Override
    public boolean isActive(DbEntryHandler objType, JSONObject objData) {
        if (!MusubiBaseActivity.getInstance().isDeveloperModeEnabled()) {
            return false;
        }
        return false;
        //return (objType instanceof VoiceObj);
    }
}
