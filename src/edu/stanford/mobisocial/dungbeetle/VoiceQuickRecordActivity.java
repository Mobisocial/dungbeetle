package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.feed.presence.Push2TalkPresence;

public class VoiceQuickRecordActivity extends Activity
        implements RemoteControlReceiver.SpecialKeyEventHandler {
    private static final String TAG = "msb-voicerecording";
	private static final String AUDIO_RECORDER_FOLDER = "DungBeetleTemp";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private Uri feedUri;
	private Set<Uri> presenceUris;
	private AudioRecord recorder = null;
	private AudioTrack track = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	private byte rawBytes[] = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_quick_recorder);

        Intent intent = getIntent();
        if (intent.hasExtra("feed_uri")) {
            feedUri = intent.getParcelableExtra("feed_uri");   
        } else if (intent.hasExtra("presence_mode")) {
            presenceUris = Push2TalkPresence.getInstance().getFeedsWithPresence();
            if (presenceUris.size() == 0) {
                presenceUris = null;
            }
        }

        if (intent.hasExtra("keydown")) {
            findViewById(R.id.sendRecord).setVisibility(View.GONE);
        }

        if (presenceUris == null && feedUri == null) {
            Toast.makeText(this, "No recipients for voice recording.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((Button)findViewById(R.id.cancelRecord)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.sendRecord)).setOnClickListener(btnClick);

        RemoteControlReceiver.setSpecialKeyEventHandler(this);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        notifyStartRecording();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RemoteControlReceiver.clearSpecialKeyEventHandler();
    }
	
	private String getTempFilename(){
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_RECORDER_FOLDER);
		
		if(!file.exists()){
			file.mkdirs();
		}
		
		File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
		
		if(tempFile.exists())
			tempFile.delete();
		
		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}

	private void notifyStartRecording() {
	    MediaPlayer player = getMediaPlayer(this, R.raw.videorecord);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                startRecording();
                mp.release();
            }
        });
	}

	private void notifySendRecording() {
        MediaPlayer player = getMediaPlayer(this, R.raw.dontpanic);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

	private void startRecording(){
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
						RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

		recorder.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				writeAudioDataToFile();
			}
		},"AudioRecorder Thread");
		recordingThread.start();
	}

	private void writeAudioDataToFile(){
		byte data[] = new byte[bufferSize];
		String filename = getTempFilename();
		FileOutputStream os = null;
		
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int read = 0;
		
		if(null != os){
			while(isRecording){
				read = recorder.read(data, 0, bufferSize);
				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void stopRecording() {
		if(null != recorder){
			isRecording = false;
			
			recorder.stop();
			recorder.release();
			
			recorder = null;
			recordingThread = null;
		}
		
		loadIntoBytes(getTempFilename());
		deleteTempFile();
	}

	private void sendRecording() {
	    if (rawBytes == null) {
	        Log.e(TAG, "No audio bytes to send");
	        finish();
	        return;
	    }
	    notifySendRecording();
	    if (feedUri != null) {
            Helpers.sendToFeed(getApplicationContext(), VoiceObj.from(rawBytes), feedUri);
        } else {
            Helpers.sendToFeeds(getApplicationContext(), VoiceObj.from(rawBytes), presenceUris);
        }

        finish();
	}

	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}
	
	private void loadIntoBytes(String inFilename) {
		FileInputStream in = null;
		long totalAudioLen = 0;
		
		try {
			in = new FileInputStream(inFilename);
			totalAudioLen = in.getChannel().size();

			rawBytes = new byte[(int)totalAudioLen];
			track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, (int)totalAudioLen, AudioTrack.MODE_STATIC);
	              
			int offset = 0;
			int numRead = 0;
			while (offset < rawBytes.length
			           && (numRead=in.read(rawBytes, offset, rawBytes.length-offset)) >= 0) {
			        offset += numRead;
		    }
			track.write(rawBytes, 0, (int)totalAudioLen);
			// TODO: Full resolution to Content Corral (if in some mode)
			//track.play();
			in.close();
		} catch (FileNotFoundException e) {
		    rawBytes = null;
		    Log.e(TAG, "Error loading audio to bytes", e);
		} catch (IOException e) {
		    rawBytes = null;
		    Log.e(TAG, "Error loading audio to bytes", e);
		} catch (IllegalArgumentException e) {
		    rawBytes = null;
		    Log.e(TAG, "Error loading audio to bytes", e);
		}
	}

	private static MediaPlayer getMediaPlayer(Context context, int resid) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) return null;

            MediaPlayer mp = new MediaPlayer();
            //mp.setAudioStreamType(AudioManager.STREAM_RING);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
           // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }
        return null;
    }
	
	private View.OnClickListener btnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
		    stopRecording();
		    if (v.getId() == R.id.cancelRecord) {
		        stopRecording();
		        finish();
		        return;
		    }

            if (rawBytes == null) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Please record a message first", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                sendRecording();
            }
		}
	};

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	    mSpecialEvent = false;
	    event.startTracking();
	    return true;
	};

	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            if ((mSpecialEvent || event.isTracking()) && isRecording) {
                stopRecording();
                sendRecording();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (!event.isCanceled()) {
                stopRecording();
                sendRecording();
            }
            return true;
        }
        return false;
    }

	private boolean mSpecialEvent = false;

    @Override
    public boolean onSpecialKeyEvent(KeyEvent event) {
        int action = event.getAction();
        mSpecialEvent = true;
        if (action == KeyEvent.ACTION_DOWN) {
            return onKeyDown(event.getKeyCode(), event);
        } else if (action == KeyEvent.ACTION_UP) {
            return onKeyUp(event.getKeyCode(), event);
        }
        return false;
    };
}