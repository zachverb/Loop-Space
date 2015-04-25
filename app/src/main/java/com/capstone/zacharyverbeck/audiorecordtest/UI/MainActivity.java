package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.capstone.zacharyverbeck.audiorecordtest.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import at.markushi.ui.CircleButton;

public class MainActivity extends Activity {

    public Button startRec, stopRec, playBack;

    public CircleButton first;

    public boolean recording;

    public String TAG = "AudioRecordTest";

    public String channelConfig;

    public int sampleRate;

    public AudioTrack mAudioTrack;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    public void init() {

        first = (CircleButton)findViewById(R.id.firstButton);

        first.setOnClickListener(startRecOnClickListener);
    }

    public View.OnClickListener startRecOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final String buttonId = v.getId() + "";
            Thread recordThread = new Thread(new Runnable(){

                @Override
                public void run() {
                    Log.d(TAG, "START REC");
                    recording = true;
                    startRecord(buttonId);
                }

            });
            recordThread.start();
            first.setOnClickListener(stopRecOnClickListener);
        }
    };

    public View.OnClickListener stopRecOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Log.d(TAG, "STOP REC");
            recording = false;
            first.setOnClickListener(playBackOnClickListener);
        }};

    public View.OnClickListener playBackOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            final String buttonId = v.getId() + "";
            Thread playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "START PLAYING");
                    playRecord(buttonId);
                }
            });
            playThread.start();
        }

    };

    private void startRecord(String id){

        File file = new File(Environment.getExternalStorageDirectory(), "test" + id + ".pcm");

        try {
            // actually create the file which is path/to/dir/test.pcm
            file.createNewFile();

            // sets up an outputstream which is a file
            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while(recording) {
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for(int i = 0; i < numberOfShort; i++){
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void playRecord(String id){

        File file = new File(Environment.getExternalStorageDirectory(), "test" + id +  ".pcm");

        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0){
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            if(mAudioTrack == null) {
                mAudioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes,
                        AudioTrack.MODE_STATIC);
            }


            mAudioTrack.write(audioData, 0, bufferSizeInBytes);

            mAudioTrack.setLoopPoints(0, audioData.length / 4, -1);

            mAudioTrack.play();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}