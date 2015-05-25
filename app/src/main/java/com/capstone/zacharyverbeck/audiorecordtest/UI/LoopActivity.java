package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.capstone.zacharyverbeck.audiorecordtest.API.S3API;
import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopProgressBar;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Loop;
import com.capstone.zacharyverbeck.audiorecordtest.Models.LoopFile;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.widgets.Dialog;
import com.gc.materialdesign.widgets.SnackBar;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

import static com.capstone.zacharyverbeck.audiorecordtest.R.color.primary_light;

public class LoopActivity extends ActionBarActivity {

    public ArrayList<Loop> mLoops;

    public int mLoopsLength = 0;

    public boolean recording;

    private boolean recordFlag = false;

    public boolean playing = false;

    public String TAG = "LoopActivity";

    public int sampleRate;

    public double bpm;

    public int beat;

    public int duration;

    public int bar;

    public int maxBars;

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

    public short[] metronomeData;

    private MaterialMenuDrawable materialMenu;

    public Toolbar playbar;

    private int selected = -1;

    private boolean bottomToolbarShowing = true;

    private int currentBeat;

    private int totalBars = 1;

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
        playing = false;
        mAudioTrack.pause();
        Log.d(TAG, "Pausing");
    }

    @Override
    public void onResume() {
        super.onResume();
        startAudio();
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
                //init();
                break;
            case R.id.action_add:
                addToLayout();
                mLoops.get(mLoopsLength - 1).getLoopButton().setOnClickListener(startRecOnClickListener);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem trash = menu.findItem(R.id.action_delete);
//        Log.d(TAG, "heh");
//        trash.setVisible(toDelete);
//        trash.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                deleteLoop(selected);
//                toDelete = false;
//                materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
//                return true;
//            }
//        });
        return true;
    }

    public void init() {
        initVariables();
        setupLayouts();
        setupToolbar();
        initAudio();
        setupRestAdapter();
        getTrackInfo();
    }

    private void initVariables() {
        trackId = getIntent().getIntExtra("trackId", -1) + "";
        sampleRate = 44100;
        bpm = (double) getIntent().getIntExtra("BPM", 60);
        Log.d(TAG, bpm + "");
        beat = (int) ((60.0/bpm) * sampleRate);
        Log.d(TAG, beat + "");
        duration = (int) ((60.0/bpm) * 1000);
        Log.d(TAG, duration + "");
        bar = beat * 4;
        Log.d(TAG, bar + "");
        maxBars = bar * 8;
        //mAudioData = new short[bar];
        try {
            metronomeData = generateMetronome();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioData = metronomeData;
    }

    private short[] generateMetronome() throws IOException {
        AssetManager assetManager = this.getAssets();
        InputStream tick = assetManager.open("tick.pcm");
        short[] tickData = new short[beat];
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(tick);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int j = 0;
            while (dataInputStream.available() > 0 && j < beat) {
                tickData[j] += (dataInputStream.readShort() * .5);
                j++;
            }

            dataInputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        short[] result = new short[bar];
        int index = 0;
        for(int i = 0; i < bar; i++) {
            if(index >= beat) {
                index = 0;
            }
            result[i] = tickData[index];
            index++;
        }
        return result;
    }

    private void setupLayouts() {
        mLeftLayout = (LinearLayout)findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout)findViewById(R.id.rightLayout);
        mLoops = new ArrayList<Loop>(6);
    }

    private void setupToolbar() {
        // top toolbar, holds navigation controls, refresh, and add track
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Current Track");
        setSupportActionBar(toolbar);

        materialMenu = new MaterialMenuDrawable(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.setIconState(MaterialMenuDrawable.IconState.ARROW);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected != -1) {
                    selected = -1;
                    materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
                    animateBottomToolbar();
                } else {
                    Log.d(TAG, "home selected");
                    Intent intent = new Intent(LoopActivity.this, TrackListActivity.class);
                    startActivity(intent);
                }
            }
        });
        toolbar.setNavigationIcon(materialMenu);

        // bottom playbar, play/pause toggle down here
        playbar = (Toolbar) findViewById(R.id.playbar);
        playbar.inflateMenu(R.menu.menu_play);
        playbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        deleteFromServer(selected);
                        selected = -1;
                        materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
                        animateBottomToolbar();
                        break;
                }
                return true;
            }
        });
        animateBottomToolbar();
    }

    private void animateBottomToolbar() {
        AnimatorSet set = new AnimatorSet().setDuration(100L);
        if(bottomToolbarShowing && selected == -1) {
            //hide toolbar
            set.playTogether(
                    ObjectAnimator.ofFloat(playbar, "translationY", 0f, 200f)
            );

            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationStart(animation);
                    playbar.setVisibility(View.GONE);
                    animation.removeAllListeners();
                }
            });

            set.setInterpolator(new LinearInterpolator());
            set.start();
            bottomToolbarShowing = false;
        } else {
            //show toolbar
            set.playTogether(
                    ObjectAnimator.ofFloat(playbar, "translationY", 200f, 0f)
            );

            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    playbar.setVisibility(View.VISIBLE);
                    animation.removeAllListeners();
                }
            });
            set.setInterpolator(new LinearInterpolator());
            set.start();
            bottomToolbarShowing = true;
        }

    }

    // initializes audiotrack and audiorecord.
    private void initAudio() {
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

    private void startAudio() {
        if(!playing) {
            playing = true;
            PlayingTask playTask = new PlayingTask();
            playTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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

    // places a button in the view.
    public void addToLayout() {
        if(mLoopsLength < 6) {
            if (mLoopsLength % 2 == 0) {
                mLeftLayout.addView(addLoopObject());
            } else {
                mRightLayout.addView(addLoopObject());
            }
            mLoopsLength++;
        } else {
            Dialog dialog = new Dialog(LoopActivity.this , "Error!", "Max amount of loops reached!");
            dialog.show();
        }
    }

    public void deleteLoop(int id) {
        Loop loop = mLoops.get(id);
        if(loop.isPlaying()) {
            deleteAudioData(loop.getAudioData());
        }
        mLoopsLength--;
        ((LinearLayout) mLoops.get(id).getContainer().getParent()).removeView(mLoops.get(id).getContainer());
        mLoops.remove(id);
        refreshLayout();
    }

    private void deleteFromServer(int id) {
         final int loopId = id;
         service.deleteLoop(trackId, mLoops.get(id).getId() + "", new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                deleteLoop(loopId);
            }

            @Override
            public void failure(RetrofitError error) {
                Dialog dialog = new Dialog(LoopActivity.this, "Permission error:", "You don't have access to delete this loop!");
                dialog.show();
                Log.d(TAG, "failed to delete loop");
                error.printStackTrace();
            }
        });
    }

    public void refreshLayout() {
        int index = 0;
        for(Loop loop : mLoops) {
            loop.setIndex(index);
            ((LinearLayout)loop.getContainer().getParent()).removeView(loop.getContainer());
            if (index % 2 == 0) {
                mLeftLayout.addView(loop.getContainer());
            } else {
                mRightLayout.addView(loop.getContainer());
            }
            index++;
        }
    }

    private RelativeLayout addLoopObject() {
        // Creating a new loop button
        LoopButton loopButton = (LoopButton) getLayoutInflater().inflate(R.layout.loop_button, null);
        //loopButton.setOnClickListener(startRecOnClickListener);
        loopButton.setId(mLoopsLength);

        // Creating the container.
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                400);
        rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(rlp);

        // setting up the parameters to add the loop button
        RelativeLayout.LayoutParams loopParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        loopParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        // adding the actual button
        relativeLayout.addView(loopButton, loopParams);

        // Adding the indeterminate progressbar
        ProgressBar progressBar = new ProgressBar(LoopActivity.this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(300,300);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(progressBar, params);

        // Adding the loop progressbar
        LoopProgressBar loopProgress = (LoopProgressBar) getLayoutInflater().inflate(R.layout.loop_circle_progress, null);
        loopProgress.setMax(1);
        loopProgress.setVisibility(View.INVISIBLE);
        loopProgress.setColor(primary_light);
        loopProgress.setDuration(duration);
        params = new RelativeLayout.LayoutParams(300,300);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(loopProgress, params);

        // Adding the loop to the global container
        mLoops.add(mLoopsLength, new Loop(relativeLayout, bar));
        mLoops.get(mLoopsLength).setIndex(mLoopsLength);
        return relativeLayout;
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
            final int index = i;
            final String endpoint = loops.get(index).endpoint;
            addToLayout();
            mLoops.get(index).setCurrentState("loading");
            mLoops.get(index).setIndex(index);
            mLoops.get(index).setId(loops.get(index).id);
            Log.d(TAG, "WE GOT HERE FAM");
            Log.d(TAG, endpoint);
            s3Service.getLoop(endpoint,
                new Callback<Response>() {
                    @Override
                    public void success(Response result, Response response) {
                        Log.d(TAG,"SUCCESS INSIDE HERE");
                        StreamingTask task = new StreamingTask();
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[]{result, index});
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

    private void setAudioData(short[] audioData) {
        mAudioData = audioData;
    }

    // Adds new audioData to the global mAudioData
    // TODO: Handle Clipping effectively.
    // PRE: takes in an array of pcm to add to the global array, and the bars it takes up
    private void addAudioData(short[] audioData, int bars) {
        int globalLength = bar * totalBars;
        int localLength = bar * bars;
        short[] globalAudioData = mAudioData;
        short[] result = new short[Math.max(globalLength, localLength)];
        int globalIndex = 0;
        int localIndex = 0;
        int index = 0;
        while(index < globalLength || index < localLength) {
            short sum = 0;
            if(globalIndex >= globalLength) {
                globalIndex = 0;
            }
            if(localIndex >= localLength) {
                localIndex = 0;
            }
            sum+=globalAudioData[globalIndex];
            sum+=audioData[localIndex];
            result[index] = sum;
            globalIndex++;
            localIndex++;
            index++;
        }
        totalBars = Math.max(totalBars, bars);
        setAudioData(result);
    }

    private void muteAudioData(short[] audioData, int bars) {
        int globalLength = bar * totalBars;
        int localLength = bar * bars;
        short[] globalAudioData = mAudioData;
        short[] result = new short[Math.max(globalLength, localLength)];
        int globalIndex = 0;
        int localIndex = 0;
        int index = 0;
        while (index < globalLength || index < localLength) {
            short sum = 0;
            if (globalIndex >= globalLength) {
                globalIndex = 0;
            }
            if (localIndex >= localLength) {
                localIndex = 0;
            }
            sum += globalAudioData[globalIndex];
            sum -= audioData[localIndex];
            result[index] = sum;
            globalIndex++;
            localIndex++;
            index++;
        }
        setAudioData(result);
    }

    private void deleteAudioData(short[] audioData) {
        findTotalBars();
        short[] globalAudioData = mAudioData;
        int globalLength = bar * totalBars;
        short[] result = new short[globalLength];
        int index = 0;
        while(index < globalLength) {
            short sum = 0;
            sum+=globalAudioData[index];
            sum-=audioData[index];
            result[index] = sum;
            index++;
        }
        setAudioData(result);
    }

    public void findTotalBars() {
        int bars = 0;
        for(Loop loop : mLoops) {
            if(loop.isPlaying()) {
                bars = Math.max(bars, loop.getBars());
            }
        }
        totalBars = bars;
    }

//    // Removes audioData from the global mAudioData
//    // TODO: Handle Clipping effectively.
//    private void muteAudioData(short[] audioData) {
//        int globalLength = mAudioData.length;
//        int localLength = audioData.length;
//        int index = 0;
//        //short[] result = new short[Math.max(mAudioData.length, audioData.length)];
//        short [] result = new short[bar];
//        // while (index < globalLength || index < localLength) {
//        while(index<bar) {
//            short sum = 0;
//            if (index < globalLength) sum += mAudioData[index];
//            if (index < localLength) sum -= audioData[index];
//            result[index] = sum;
//            index++;
//        }
//        setAudioData(result);
//    }

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
            recordFlag = true;
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
        }
    };

    // toggles individual audio tracks to play/not play
    public View.OnClickListener togglePlayOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Loop loop = mLoops.get(v.getId());
            if(loop.isPlaying()) {
                //necessary to set it before checking the total bars in the subtractMethod
                mLoops.get(v.getId()).setIsPlaying(false);
                muteAudioData(loop.getAudioData(), loop.getBars());
                loop.setCurrentState("paused");
            } else {
                mLoops.get(v.getId()).setIsPlaying(true);
                addAudioData(loop.getAudioData(), loop.getBars());
                loop.setCurrentState("playing");
            }
        }

    };

    private View.OnLongClickListener setLoopButtonSelected = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(100);
            if(selected == v.getId()) {
                selected = -1;
                materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
            } else {
                selected = v.getId();
                materialMenu.animateIconState(MaterialMenuDrawable.IconState.X);
            }
            animateBottomToolbar();
            return true;
        }
    };

    /*
     * ASYNC TASKS
     */

    private class PlayingTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            Log.d(TAG, "THREAD PLAYIN NOW");
            mAudioTrack.play();
            Log.d(TAG, "PLAY");
            currentBeat = 1;
            while(playing) {
                if(recordFlag == true) {
                    currentBeat = 0;
                    for(int i = 1; i <= 8; i++) {
                        if(i % 2 == 1) {
                            publishProgress(0);
                        }
                        mAudioTrack.write(metronomeData, ((beat / 2) * (i - 1)), (beat / 2));
                    }
                    currentBeat = 1;
                    recordFlag = false;
                }
                // totalBars is the number of bars. we are writing twice every
                // down beat for speed, so current beat is equivalent to 4 bars * 2
                if(currentBeat > (totalBars * 8)) {
                    currentBeat = 1;
                }
                // This is each time there is a down beat.
                if(currentBeat % 2 == 1) {
                    publishProgress((currentBeat/2) + 1);
                }
                mAudioTrack.write(mAudioData, ((beat / 2) * (currentBeat - 1)), (beat / 2));
                currentBeat++;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... param) {
            setLoopsProgress(param[0]);
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
                while(currentBeat != 1) {
                    try
                    {
                        Thread.sleep(10);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                mAudioRecord.startRecording();
                publishProgress(new Integer[]{1, id});
                while(recording) {
                    int numberOfShort = mAudioRecord.read(tempAudioData, 0, minBufferSize);

                    for(int i = 0; i < numberOfShort; i++){
                        dataOutputStream.writeShort(tempAudioData[i]);
                    }
                }

                mAudioRecord.stop();
                dataOutputStream.close();
                publishProgress(new Integer[]{-1, id});

                mLoops.get(id).setFilePath(file.getAbsolutePath());

                TypedFile soundFile = new TypedFile("binary", file);

                service.upload(soundFile, new Callback<Endpoint>() {
                    @Override
                    public void success(Endpoint data, Response response) {
                        if(data.type == true) {
                            Loop loop = mLoops.get(id);
                            loop.setEndpoint(data.endpoint);
                            loop.setId(data.id);
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
            if(params[0] == -1) {
                setLoopLoading(params[1]);
            } else {
                setRecordingImage(params[1]);
            }
        }

        @Override
        protected void onPostExecute(Integer index) {
            taskPostExecute(index);
        }

    }

    private class StreamingTask extends AsyncTask<Object, Integer, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            Log.d(TAG, "HELP");
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
                mLoops.get(index).setFilePath(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return index;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            setLoopLoading(params[0]);
        }

        @Override
        protected void onPostExecute(Integer index) {
            taskPostExecute(index);
        }
    }

    public void setRecordingImage(Integer index) {
        mLoops.get(index).setCurrentState("recording");
    }

    public void setLoopLoading(Integer index) {
        mLoops.get(index).setCurrentState("loading");
    }

    public void taskPostExecute(Integer index) {
        Loop loop = mLoops.get(index);
        LoopButton loopButton = loop.getLoopButton();
        addAudioData(loop.getAudioData(), loop.getBars());
        loopButton.setOnClickListener(togglePlayOnClickListener);
        loopButton.setOnLongClickListener(setLoopButtonSelected);
        loop.setCurrentState("playing");
    }

    public void setLoopsProgress(Integer value) {
        for(int i = 0; i < mLoopsLength; i++) {
            mLoops.get(i).setLoopProgress(value);
        }
    }
}