package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VoiceObj;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import android.content.Intent;
import android.util.Log;

public class VoiceRecorderActivity extends Activity {
    private static final String AUDIO_RECORDER_FOLDER = "DungBeetleTemp";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Uri feedUri;
    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private byte rawBytes[] = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_recorder);
        
        setButtonHandlers();
        enableButtons(false);

        Intent intent = getIntent();
        feedUri = Uri.parse(intent.getStringExtra(FeedViewFragment.ARG_FEED_URI));
        
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
    }

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.startRecord)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.stopRecord)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.sendRecord)).setOnClickListener(btnClick);
    }
    
    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }
    
    private void enableButtons(boolean isRecording) {
        enableButton(R.id.startRecord,!isRecording);
        enableButton(R.id.stopRecord,isRecording);
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
    
    private void stopRecording(){
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

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        
        file.delete();
    }
    
    private void loadIntoBytes(String inFilename){
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

            track.play();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.startRecord:
                    
                    enableButtons(true);
                    startRecording();
                            
                    break;
                    
                case R.id.stopRecord:
                    
                    enableButtons(false);
                    stopRecording();
                    
                    break;
                    
                case R.id.sendRecord:
                    if(rawBytes == null)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please record a message first", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else
                    {
                        Helpers.sendToFeed(
                            getApplicationContext(), VoiceObj.from(rawBytes), feedUri);

                        Log.i("voice recorder",feedUri.toString());
                        finish();
                    }
                    break;
            }
        }
    }; 
}