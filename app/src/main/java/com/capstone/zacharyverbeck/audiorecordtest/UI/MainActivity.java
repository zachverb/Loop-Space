package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.capstone.zacharyverbeck.audiorecordtest.Buttons.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Loop;
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

public class MainActivity extends Activity {

    public Loop[] mLoops;

    public Button mPlayButton;

    public boolean recording;

    public boolean playing = false;

    public String TAG = "AudioRecordTest";

    public int sampleRate = 44100;

    public AudioTrack mAudioTrack;

    public AudioRecord mAudioRecord;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(playing) {
            mAudioTrack.pause();
        }
    }

    public void init() {
        mLoops = new Loop[2];
        mLoops[0] = (new Loop((LoopButton) findViewById(R.id.firstButton)));
        mLoops[1] = (new Loop((LoopButton) findViewById(R.id.secondButton)));
        for(int i = 0; i < 2; i++) {
            LoopButton button = mLoops[i].getLoopButton();
            button.setOnClickListener(startRecOnClickListener);
        }

        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playBackOnClickListener);

    }

    public View.OnClickListener startRecOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final int buttonId = v.getId();
            Log.d(TAG, "START REC");
            recording = true;
            RecordingTask task = new RecordingTask();
            task.execute(buttonId);
            v.setOnClickListener(stopRecOnClickListener);
        }
    };

    public View.OnClickListener stopRecOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Log.d(TAG, "STOP REC");
            recording = false;
            Loop loop = findLoopById(v.getId());
            loop.getLoopButton().setText(loop.getName(), 64.0f, Color.WHITE);
            v.setOnClickListener(startRecOnClickListener);
        }};

    public View.OnClickListener playBackOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Thread playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "START PLAYING");
                    playRecord();
                }
            });
            playThread.start();
        }

    };

    public void playRecord(){
        if(!playing) {
            playing = true;

            File files[] = new File[mLoops.length];
            int shortSizeInBytes = Short.SIZE / Byte.SIZE;
            int bufferSizeInBytes = 0;

            for (int i = 0; i < 2; i++) {
                Log.d(TAG, mLoops[i].getId() + "");
                files[i] = new File(Environment.getExternalStorageDirectory(), "test" + mLoops[i].getId() + ".pcm");
                int length = (int) (files[i].length() / shortSizeInBytes);
                Log.d(TAG, length + "");
                if(bufferSizeInBytes < length) {
                    Log.d(TAG, "Larger.");
                    bufferSizeInBytes = length;
                }
            }

            short[] audioData = new short[bufferSizeInBytes];

            for(int i = 0; i < 2; i++) {

                try {
                    InputStream inputStream = new FileInputStream(files[i]);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                    DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

                    int j = 0;
                    while (dataInputStream.available() > 0) {
                        audioData[j] += (dataInputStream.readShort() * .5);
                        j++;
                    }

                    dataInputStream.close();


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mAudioTrack == null) {
                mAudioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes,
                        AudioTrack.MODE_STREAM);
            }


            mAudioTrack.write(audioData, 0, bufferSizeInBytes);

            //mAudioTrack.setLoopPoints(0, audioData.length / 4, -1);

            mAudioTrack.play();
        } else {
            playing = false;
            mAudioTrack.stop();
            mAudioTrack.flush();
        }
    }

    private class RecordingTask extends AsyncTask<Integer, Integer, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            int id = params[0];

            File file = new File(Environment.getExternalStorageDirectory(), "test" + id + ".pcm");

            if (file.exists())
                file.delete();
            try {
                // actually create the file which is path/to/dir/test.pcm
                file.createNewFile();

                // sets up an outputstream which is a file
                OutputStream outputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                //sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);

                int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);


                if(mAudioRecord == null) {
                    mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufferSize);
                }

                short[] audioData = new short[minBufferSize];

                mAudioRecord.startRecording();
                publishProgress(id);

                while(recording) {
                    int numberOfShort = mAudioRecord.read(audioData, 0, minBufferSize);
                    for(int i = 0; i < numberOfShort; i++){
                        dataOutputStream.writeShort(audioData[i]);
                    }
                }

                mAudioRecord.stop();
                dataOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            LoopButton loopButton = (LoopButton)findViewById(params[0]);
            loopButton.setText("Recording", 64.0f, Color.WHITE);
        }

    }

    public Loop findLoopById(int id) {
        for(int i = 0; i < mLoops.length; i++) {
            if(mLoops[i].getId() == id) {
                return mLoops[i];
            }
        }
        return null;
    }
}