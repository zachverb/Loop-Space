package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.capstone.zacharyverbeck.audiorecordtest.API.S3API;
import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Buttons.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
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
import java.util.PriorityQueue;
import java.util.Queue;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

public class MainActivity extends Activity {

    public Loop[] mLoops;

    public int mLoopsLength = 0;

    public Button mPlayButton;

    public boolean recording;

    public boolean playing = false;

    public String TAG = "AudioRecordTest";

    public int sampleRate = 44100;

    public AudioTrack mAudioTrack;

    public AudioRecord mAudioRecord;

    public LinearLayout mLeftLayout;

    public LinearLayout mRightLayout;

    public ServerAPI service;

    public S3API s3Service;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setupRestAdapter();
    }

    private void setupRestAdapter() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                //.setRequestInterceptor(interceptor)
                .build();

        service = restAdapter.create(ServerAPI.class);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(playing) {
            mAudioTrack.pause();
        }
    }

    public void init() {
        mLeftLayout = (LinearLayout)findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout)findViewById(R.id.rightLayout);
        mLoops = new Loop[6];
        addButton();
        addButton();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://s3-us-west-2.amazonaws.com/loopspace/")
                .build();
        s3Service = restAdapter.create(S3API.class);

        s3Service.getLoop("cf0271b3472f1d0cf18e-test0.pcm",
                new Callback<Response>() {
                    @Override
                    public void success(Response result, Response response) {
                        File file = new File(Environment.getExternalStorageDirectory(), "cf0271b3472f1d0cf18e-test0.pcm");
                        try {
                            InputStream inputStream = response.getBody().in();
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

                            OutputStream outStream = new FileOutputStream(file);
                            byte[] buffer = new byte[8 * 1024];

                            while (dataInputStream.available() > 0) {
                                outStream.write(buffer, 0, inputStream.read(buffer));
                            }

                            outStream.close();
                            dataInputStream.close();
                            mLoops[0].setFilePath(file.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.d(TAG, "FAK");
                        retrofitError.printStackTrace();
                    }
                });

        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playBackOnClickListener);

    }

    public LoopButton newLoopButton() {
        LayoutInflater inflater = getLayoutInflater();
        LoopButton loopButton = (LoopButton) inflater.inflate(R.layout.loop_button, null);
        loopButton.setOnClickListener(startRecOnClickListener);
        loopButton.setId(mLoopsLength);
        return loopButton;
    }

    public void addButton() {
        LoopButton loopButton = newLoopButton();
        mLoops[mLoopsLength] = new Loop(loopButton);
        mLoops[mLoopsLength].setId(loopButton.getId());
        if(mLoopsLength % 2 == 0) {
            mLeftLayout.addView(loopButton);
        } else {
            mRightLayout.addView(loopButton);
        }
        mLoopsLength++;
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
            addButton();
            TypedFile soundFile = new TypedFile("audio/x-wav;codec=pcm;rate=44100", new File(mLoops[v.getId()].getFilePath()));
            Log.d("Spacers", "uploadin dat sheeit");
            service.upload(soundFile, new Callback<Endpoint>() {
                @Override
                public void success(Endpoint data, Response response) {
                    if(data.type == true) {
                        Log.d("Spacers", data.endpoint);
                    } else {
                        Log.d("Spacers", "even worse");
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    retrofitError.printStackTrace();
                }
            });
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

            Queue<File> files= new PriorityQueue<>();
            int shortSizeInBytes = Short.SIZE / Byte.SIZE;
            int bufferSizeInBytes = 0;


            for(int i = 0; i < mLoopsLength; i++) {
                Log.d(TAG, mLoops[i].getId() + "");
                if(mLoops[i].getFilePath() != null) {
                    File file = new File(mLoops[i].getFilePath());
                    files.add(file);
                    int length = (int) (file.length() / shortSizeInBytes);
                    Log.d(TAG, length + "");
                    if (bufferSizeInBytes < length) {
                        Log.d(TAG, "Larger.");
                        bufferSizeInBytes = length;
                    }
                }
            }

            short[] audioData = new short[bufferSizeInBytes];

            int i = 0;
            while(files.size() > 0) {

                try {
                    InputStream inputStream = new FileInputStream(files.remove());
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

                i++;

            }

            if (mAudioTrack == null) {
                mAudioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes,
                        AudioTrack.MODE_STATIC);
            }


            mAudioTrack.write(audioData, 0, bufferSizeInBytes);

            mAudioTrack.setLoopPoints(0, audioData.length / 2, -1);

            mAudioTrack.play();
        } else {
            playing = false;
            mAudioTrack.pause();
        }
    }

    private class RecordingTask extends AsyncTask<Integer, Integer, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            int id = params[0];

            File file = new File(Environment.getExternalStorageDirectory(), "test" + id + ".pcm");
            mLoops[id].setFilePath(file.getAbsolutePath());
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

                while(recording) {
                    int numberOfShort = mAudioRecord.read(audioData, 0, minBufferSize);
                    publishProgress(id);
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
            LoopButton loopButton = (LoopButton) findViewById(params[0]);
            loopButton.setImageResource(R.mipmap.microphone_filled);
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