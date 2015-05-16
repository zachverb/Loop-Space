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
import android.widget.LinearLayout;

import com.capstone.zacharyverbeck.audiorecordtest.API.S3API;
import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Loop;
import com.capstone.zacharyverbeck.audiorecordtest.Models.LoopFile;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.widgets.Dialog;
import com.gc.materialdesign.widgets.SnackBar;

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

    public boolean recording;

    public boolean playing = false;

    public String TAG = "LoopActivity";

    public int sampleRate = 44100;

    public int bpm = 60;

    public int beat = (60/bpm) * sampleRate;

    public int bar = beat * 4;

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

        setupLayouts();
        setupToolbar();
        audioInit();
        setupRestAdapter();
        getTrackInfo();


    }

    private void setupLayouts() {
        mLeftLayout = (LinearLayout)findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout)findViewById(R.id.rightLayout);
        mLoops = new Loop[6];
        trackId = getIntent().getIntExtra("trackId", -1) + "";
        // bpm = getIntent().getIntExtra("BPM", 80);
    }

    private void setupToolbar() {
        // top toolbar, holds navigation controls, refresh, and add track
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

        // bottom playbar, play/pause toggle down here
        Toolbar playbar = (Toolbar) findViewById(R.id.playbar);
        playbar.inflateMenu(R.menu.menu_play);
        playbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                if (playing) {
                    menuItem.setIcon(R.drawable.ic_play_arrow_white_24dp);
                } else {
                    menuItem.setIcon(R.drawable.ic_pause_white_24dp);
                }
                Thread playThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "START PLAY THREAD");
                        playRecord();
                    }
                });
                playThread.start();
                return true;
            }
        });
    }

    // initializes audiotrack and audiorecord.
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

        mAudioData = new short[beat];
//        double[] tick = getSineWave((sampleRate / 16), sampleRate, 880);
//        short[] lol = get16BitPcm(tick);
//        for(int i = 0; i < lol.length; i++) {
//            Log.d(TAG, lol[i] + "");
//        }
//        addAudioData(lol);
    }

    // sets up the REST Client for the AWS s3 server and the node.js server.
    private void setupRestAdapter() {

        // Find token.
        settings = PreferenceManager
            .getDefaultSharedPreferences(this.getApplicationContext());
        final String token = settings.getString("token", "");
        if(token == "") {
            Dialog dialog = new Dialog(getApplicationContext() , "Error!", "You must be signed in!");
            dialog.show();
            Intent intent = new Intent(LoopActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        // set up heroku connection header with the token and trackId.
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", token);
                request.addHeader("track_id", trackId);
            }
        };

        // sets up connection to heroku server
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

    private void setupHowTo() {
        final SnackBar snackbar;
        snackbar = new SnackBar(LoopActivity.this, "Press the + button to add a new loop!");
        snackbar.show();
    }


    /*
     *  BUTTON CREATION FUNCTIONS
     */

    // returns a new LoopButton object ready to put in the view.
    public LoopButton newLoopButton() {
        LayoutInflater inflater = getLayoutInflater();
        LoopButton loopButton = (LoopButton) inflater.inflate(R.layout.loop_button, null);
        loopButton.setOnClickListener(startRecOnClickListener);
        loopButton.setId(mLoopsLength);
        return loopButton;
    }

    // places a button in the view.
    public void addButton() {
        if(mLoopsLength < 6) {
            LoopButton loopButton = newLoopButton();
            mLoops[mLoopsLength] = new Loop(loopButton);
            mLoops[mLoopsLength].setId(loopButton.getId());
            if (mLoopsLength % 2 == 0) {
                mLeftLayout.addView(loopButton);
            } else {
                mRightLayout.addView(loopButton);
            }
            mLoopsLength++;
        }
    }

    /*
     *  SYNC WITH SERVER
     */

    // gets the endpoints for each track
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

    // downloads each track from their given endpoint.
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

    /*
     *  GLOBAL AUDIO FUNCTIONS
     */

    private short[] getAudioData() {
        return mAudioData;
    }

    private void setAudioData(short[] audioData) {
        mAudioData = audioData;
    }

    // Adds new audioData to the global mAudioData
    // TODO: Handle Clipping effectively.
    private void addAudioData(short[] audioData) {
        int globalLength = mAudioData.length;
        int localLength = audioData.length;
        int index = 0;
        // short [] result = new short[Math.max(mAudioData.length, audioData.length)];
        short [] result = new short[bar];
        // while(index < globalLength || index < localLength) {
        while(index<bar) {
            short sum = 0;
            if(index < globalLength) sum+=mAudioData[index];
            if(index < localLength) sum+=audioData[index];
            result[index] = sum;
            index++;
        }
        setAudioData(result);
    }

    // Removes audioData from the global mAudioData
    // TODO: Handle Clipping effectively.
    private void subtractAudioData(short[] audioData) {
        int globalLength = mAudioData.length;
        int localLength = audioData.length;
        int index = 0;
        //short[] result = new short[Math.max(mAudioData.length, audioData.length)];
        short [] result = new short[bar];
        // while (index < globalLength || index < localLength) {
        while(index<bar) {
            short sum = 0;
            if (index < globalLength) sum += mAudioData[index];
            if (index < localLength) sum -= audioData[index];
            result[index] = sum;
            index++;
        }
        setAudioData(result);
    }

    /*
     *  ONCLICK LISTENERS
     */

    // Initial recording OnClickListener
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

    // Stop the recording of the button.
    public View.OnClickListener stopRecOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG, "STOP REC");
            recording = false;
            v.setOnClickListener(togglePlayOnClickListener);
        }
    };

    // toggles individual audio tracks to play/not play
    public View.OnClickListener togglePlayOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Loop loop = mLoops[v.getId()];
            if(loop.isPlaying()) {
                subtractAudioData(loop.getAudioData());
                loop.getLoopButton().setImageResource(R.drawable.ic_play_arrow_white_48dp);
            } else {
                addAudioData(loop.getAudioData());
                loop.getLoopButton().setImageResource(R.drawable.ic_pause_white_48dp);
            }
            mLoops[v.getId()].setIsPlaying(!loop.isPlaying());
        }

    };


    // Function that plays/pauses the audioTrack
    public void playRecord(){
        if(!playing) {
            mAudioTrack.play();
            Log.d(TAG, "PLAY");
            playing = true;
            int currentBeat = 1;
            while(playing) {
                if(beat * currentBeat > mAudioData.length) {
                    currentBeat = 1;
                }
                //short[] audioData = getAudioData();
                Log.d(TAG, "Play " + mAudioData.length + " Position: " + mAudioTrack.getPlaybackHeadPosition());
                mAudioTrack.write(mAudioData, (beat * (currentBeat - 1)), (beat * (currentBeat++)));
            }
        } else {
            Log.d(TAG, "PAUSE");
            playing = false;
            mAudioTrack.pause();
        }
    }

    /*
     * ASYNC TASKS
     */

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
                publishProgress(id);
                while(recording) {
                    int numberOfShort = mAudioRecord.read(tempAudioData, 0, minBufferSize);

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
            setRecordingImage(params[0]);
        }

        @Override
        protected void onPostExecute(Integer index) {
            taskPostExecute(index);
        }

    }

    private class StreamingTask extends AsyncTask<Object, Integer, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            final Response res = (Response) params[0];
            final int index = (int) params[1];
            publishProgress(index);
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
            setRecordingImage(params[0]);
        }

        @Override
        protected void onPostExecute(Integer index) {
            taskPostExecute(index);
        }
    }

    public void setRecordingImage(Integer index) {
        LoopButton loopButton = mLoops[index].getLoopButton();
        loopButton.setImageResource(R.drawable.ic_mic_white_48dp);
        //loopButton.setAnimation(AnimationUtils.loadAnimation(this, R.anim.progress_indeterminate_animation));
    }

    public void taskPostExecute(Integer index) {
        LoopButton loopButton = mLoops[index].getLoopButton();
        loopButton.setImageResource(R.drawable.ic_pause_white_48dp);
        addAudioData(mLoops[index].getAudioData());
        loopButton.setOnClickListener(togglePlayOnClickListener);
    }

    // Metronome methods

    public double[] getSineWave(int samples,int sampleRate,double frequencyOfTone){
        double[] sample = new double[samples];
        for (int i = 0; i < samples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequencyOfTone));
        }
        return sample;
    }

    public short[] get16BitPcm(double[] samples) {
        short[] generatedSound = new short[samples.length];
        int index = 0;
        for (double sample : samples) {
            // scale to maximum amplitude
            short maxSample = (short) (sample * Short.MAX_VALUE);
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[index++] = maxSample;
            //generatedSound[index++] = maxSample;

        }
        return generatedSound;
    }
}