package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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

    public boolean hasChanged;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_refresh:
                getTrackInfo();
                break;
            case R.id.action_add:
                addButton();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void init() {

        setupToolbar();

        mLeftLayout = (LinearLayout)findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout)findViewById(R.id.rightLayout);
        mLoops = new Loop[6];
        trackId = getIntent().getIntExtra("trackId", -1) + "";

        hasChanged = false;

        audioInit();
        setupRestAdapter();
        getTrackInfo();

        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playBackOnClickListener);

        mAudioData = new short[minBufferSize];
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Current Track");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "home selected");
                Intent intent = new Intent(LoopActivity.this, TrackListActivity.class);
                startActivity(intent);
            }
        });
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

    private void setAudioData(short[] audioData) {
        mAudioData = audioData;
    }

    private void addAudioData(short[] audioData) {
        int globalLength = mAudioData.length;
        int localLength = audioData.length;
        int index = 0;
        short [] result = new short[Math.max(mAudioData.length, audioData.length)];
        while(index < globalLength || index < localLength) {
            short sum = 0;
            if(index < globalLength) sum+=mAudioData[index];
            if(index < localLength) sum+=audioData[index];
            result[index] = sum;
            index++;
        }
        setAudioData(result);
        hasChanged = true;
    }

    private void subtractAudioData(short[] audioData) {
        int a = mAudioData.length-1;
        int b = audioData.length-1;

        short [] result = new short[Math.max(mAudioData.length, audioData.length)];
        short sum = 0;
        while(a >= 0 || b >= 0) {
            if(a>=0) sum-=mAudioData[a];
            if(b>=0) sum-=audioData[b];

            result[Math.max(a, b)] = sum;
            sum = 0;
            a--;
            b--;
        }
        setAudioData(result);
        hasChanged = true;
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
            v.setOnClickListener(addAudioDataOnClickListener);
        }};

    public View.OnClickListener playBackOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Thread playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "START PLAY THREAD");
                    playRecord();
                }
            });
            playThread.start();
        }

    };

    public View.OnClickListener addAudioDataOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            // todo
            // write code to add individual AudioData to the play stream.
        }

    };

    public void playRecord(){
        if(!playing) {
            mAudioTrack.play();
            Log.d(TAG, "PLAY");
            playing = true;
//
//            int shortSizeInBytes = Short.SIZE / Byte.SIZE;
//            int bufferSizeInBytes = 0;
//
//            Queue<File> files = new PriorityQueue<>();
//            for(int i = 0; i < mLoopsLength; i++) {
//                Log.d(TAG, mLoops[i].getId() + "");
//                if(mLoops[i].getFilePath() != null) {;
//                    File file = new File(mLoops[i].getFilePath());
//                    files.add(file);
//                    int length = (int) (file.length() / shortSizeInBytes);
//                    Log.d(TAG, length + "");
//                    if (bufferSizeInBytes < length) {
//                        bufferSizeInBytes = length;
//                    }
//                }
//            }
//
//
//            setAudioData(new short[bufferSizeInBytes]);
//
//            while(files.size() > 0) {
//                try {
//                    InputStream inputStream = new FileInputStream(files.remove());
//                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
//                    DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
//
//                    int j = 0;
//                    while (dataInputStream.available() > 0) {
//                        mAudioData[j] += (dataInputStream.readShort() * .5);
//                        j++;
//                    }
//
//                    dataInputStream.close();
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

//          }
            while(playing) {
                short[] audioData = getAudioData();
                Log.d(TAG, "Play " + audioData.length + " Position: " + mAudioTrack.getPlaybackHeadPosition());
                mAudioTrack.write(audioData, 0, audioData.length);
            }
        } else {
            Log.d(TAG, "PAUSE");
            playing = false;
            mAudioTrack.pause();
        }
    }

    private class RecordingTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            final int id = params[0];

            File file = new File(Environment.getExternalStorageDirectory(), "test" + id + ".pcm");
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

                mLoops[id].setFilePath(file.getAbsolutePath());


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
            return id;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            LoopButton loopButton = (LoopButton) findViewById(params[0]);
            loopButton.setImageResource(R.drawable.ic_mic_white_48dp);
        }

        @Override
        protected void onPostExecute(Integer index) {
            LoopButton loopButton = mLoops[index].getLoopButton();
            loopButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            loopButton.setOnClickListener(addAudioDataOnClickListener);
            addAudioData(mLoops[index].getAudioData());
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
                mLoops[index].setFilePath(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return index;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            LoopButton loopButton = mLoops[params[0]].getLoopButton();
            loopButton.setImageResource(R.drawable.ic_mic_white_48dp);
        }

        @Override
        protected void onPostExecute(Integer index) {
            LoopButton loopButton = mLoops[index].getLoopButton();
            loopButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            loopButton.setOnClickListener(addAudioDataOnClickListener);
            addAudioData(mLoops[index].getAudioData());
        }

    }
}