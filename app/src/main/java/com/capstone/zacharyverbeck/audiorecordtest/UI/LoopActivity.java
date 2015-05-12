package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.capstone.zacharyverbeck.audiorecordtest.API.S3API;
import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Loop;
import com.capstone.zacharyverbeck.audiorecordtest.Models.LoopFile;
import com.capstone.zacharyverbeck.audiorecordtest.R;

import org.apache.commons.io.IOUtils;

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
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

public class LoopActivity extends ActionBarActivity {

    public Loop[] mLoops;

    public int mLoopsLength = 0;

    public Button mPlayButton;

    public boolean recording;

    public boolean playing = false;

    public String TAG = "LoopActivity";

    public int sampleRate = 44100;

    public int bpm = 60;

    public int bar = sampleRate * 4;

    public int minBufferSize;

    public AudioTrack mAudioTrack;

    public AudioRecord mAudioRecord;

    public LinearLayout mLeftLayout;

    public LinearLayout mRightLayout;

    public ServerAPI service;

    public S3API s3Service;

    public SharedPreferences settings;

    public String trackId;

    public short[] mAudioData;

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
            playing = false;
            mAudioTrack.pause();
        }
    }

    public void init() {
        mLeftLayout = (LinearLayout)findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout)findViewById(R.id.rightLayout);
        mLoops = new Loop[6];
        trackId = getIntent().getIntExtra("trackId", -1) + "";
        audioInit();
        setupRestAdapter();
        getTrackInfo();
        addButton();

        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playBackOnClickListener);
    }

    private void audioInit() {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM);
    }

    private void setupRestAdapter() {
        settings = PreferenceManager
            .getDefaultSharedPreferences(this.getApplicationContext());

        final String token = settings.getString("token", "");
        // setup heroku connection
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", token);
                request.addHeader("track_id", trackId);
            }
        };
        RestAdapter serverRestAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .setRequestInterceptor(interceptor)
                .build();
        service = serverRestAdapter.create(ServerAPI.class);

        // setup s3 connection
        RestAdapter s3RestAdapter = new RestAdapter.Builder()
                .setEndpoint("https://s3-us-west-2.amazonaws.com/loopspace/")
                .build();
        s3Service = s3RestAdapter.create(S3API.class);
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

    private void getTrackInfo() {
        service.getLoops(trackId,
            new Callback<List<LoopFile>>() {
                @Override
                public void success(List<LoopFile> data, Response response) {
                    downloadLoops(data);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.d(TAG, "failed grabbing all loop endpoints");
                    retrofitError.printStackTrace();
                }
            });
    }

    private short[] getAudioData() {

        return mAudioData;
    }

    public void downloadLoops(List<LoopFile> loops) {
        for (int i = 0; i < loops.size(); i++) {
            addButton();
            final int index = i;
            final String endpoint = loops.get(i).endpoint;

            Log.d(TAG, endpoint);
            s3Service.getLoop(endpoint,
                    new Callback<Response>() {
                        @Override
                        public void success(Response result, Response response) {
                            StreamingTask task = new StreamingTask();
                            task.execute(new Object[]{result, index});
                        }


                        @Override
                        public void failure(RetrofitError retrofitError) {
                            Log.d(TAG, "FAK");
                            retrofitError.printStackTrace();
                        }
                    });
        }
    }

    private short[] getAudioDataFromFile(File file) {
        int shortSizeInBytes = Short.SIZE / Byte.SIZE;
        short[] audioData = new short[(int) (file.length() / shortSizeInBytes)];

        try {
            InputStream inputStream = new FileInputStream(file);
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
//        for(int i = 0; i < 500; i++) {
//            Log.d(TAG, audioData[i] + "");
//        }
        return audioData;
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
            Log.d("Spacers", "uploadin dat sheeit");
            //v.setOnClickListener(startRecOnClickListener);
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
            mAudioTrack.play();
            playing = true;

            int shortSizeInBytes = Short.SIZE / Byte.SIZE;
            int bufferSizeInBytes = 0;
            Log.d(TAG, "YO");
//            short[] tempAudioData = mLoops[0].getAudioData();
//            for(int i = 0; i < tempAudioData.length; i++) {
//                Log.d(TAG, tempAudioData[i] + "");
//            }
           // Queue<short[]> audioDataContainer = new PriorityQueue<>();
            Queue<File> files = new PriorityQueue<>();
            for(int i = 0; i < mLoopsLength; i++) {
//                short[] data = mLoops[i].getAudioData();
//                Log.d(TAG, "We out here");
//                if(mLoops[i].getAudioData() != null) {
//                    Log.d(TAG, "We a lil deeper" + data.length);
//                    audioDataContainer.add(data);
//                    if(data.length > bufferSizeInBytes) {
//                        bufferSizeInBytes = data.length;
//                        Log.d(TAG, "We in it to win it");
//                    }
//
//                }
                Log.d(TAG, mLoops[i].getId() + "");
                if(mLoops[i].getFilePath() != null) {
                    Log.d(TAG, mLoops[i].getFilePath() + " DO U BELIEVE ME");
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

            while(files.size() > 0) {
//                short[] tempAudioData = audioDataContainer.remove();
//                Log.d(TAG, "i = " + (i + 1));
//                for(int j = 0; j < tempAudioData.length; j++) {
//                    audioData[j] += tempAudioData[j];
//                }
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

            }
            int size;
            if(bar < bufferSizeInBytes) {
                size = bufferSizeInBytes;
            } else {
                size = bar;
            }
            while(playing) {
                mAudioTrack.write(audioData, 0, audioData.length);
            }



        } else {
            playing = false;
            mAudioTrack.pause();
        }
    }

    private class RecordingTask extends AsyncTask<Integer, Integer, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            final int id = params[0];

            File file = new File(Environment.getExternalStorageDirectory(), "test" + id + ".pcm");
            mLoops[id].setFilePath(file.getAbsolutePath());
            try {
                // actually create the file which is path/to/dir/test.pcm
                file.createNewFile();

                // sets up an outputstream which is a file
                OutputStream outputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                short[] tempAudioData = new short[minBufferSize];

                mAudioRecord.startRecording();

                while(recording) {
                    int numberOfShort = mAudioRecord.read(tempAudioData, 0, minBufferSize);
                    publishProgress(id);
                    for(int i = 0; i < numberOfShort; i++){
                        dataOutputStream.writeShort(tempAudioData[i]);
                    }
                }

                mAudioRecord.stop();
                dataOutputStream.close();

                mLoops[id].setAudioData(tempAudioData);

                TypedFile soundFile = new TypedFile("binary", file);

                Log.d(TAG, soundFile.mimeType());

                service.upload(soundFile, new Callback<Endpoint>() {
                    @Override
                    public void success(Endpoint data, Response response) {
                        if(data.type == true) {
                            mLoops[id].setEndpoint(data.endpoint);
                        } else {
                            Log.d("Spacers", "Something went wrong.");
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        retrofitError.printStackTrace();
                    }
                });
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

    private class StreamingTask extends AsyncTask<Object, Integer, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            final Response res = (Response) params[0];
            final int index = (int) params[1];
            try {
                File file = new File(Environment.getExternalStorageDirectory(), "downloaded" + (index + 1) + ".pcm");
                InputStream inputStream = res.getBody().in();
                OutputStream out = new FileOutputStream(file);
                IOUtils.copy(inputStream, out);
                inputStream.close();
                out.close();
                mLoops[index].setAudioData(getAudioDataFromFile(file));
                mLoops[index].setFilePath(file.getAbsolutePath());
                //mLoops[index].setAudioData(audioData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return index;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            //LoopButton loopButton = (LoopButton) findViewById(params[0]);
            //loopButton.setImageResource(R.mipmap.microphone_filled);
        }

        @Override
        protected void onPostExecute(Integer index) {
            mLoops[index].getLoopButton().setImageResource(R.mipmap.microphone_filled);
        }

    }
}