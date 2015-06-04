package com.capstone.zacharyverbeck.loopspace.UI;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.capstone.zacharyverbeck.loopspace.API.S3API;
import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Java.LoopApplication;
import com.capstone.zacharyverbeck.loopspace.Java.LoopButton;
import com.capstone.zacharyverbeck.loopspace.Java.LoopProgressBar;
import com.capstone.zacharyverbeck.loopspace.Java.SimpleDiskCache;
import com.capstone.zacharyverbeck.loopspace.Models.Endpoint;
import com.capstone.zacharyverbeck.loopspace.Models.Loop;
import com.capstone.zacharyverbeck.loopspace.Models.LoopFile;
import com.capstone.zacharyverbeck.loopspace.R;
import com.gc.materialdesign.widgets.Dialog;
import com.gc.materialdesign.widgets.SnackBar;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import static com.capstone.zacharyverbeck.loopspace.R.color.primary_light;

public class LoopActivity extends ActionBarActivity {

    public ArrayList<Loop> mLoops;
    public int mLoopsLength = 0;
    public boolean recording;
    private boolean recordFlag = false;
    public boolean playing = false;
    public String TAG = "LoopActivity";
    public int sampleRate;
    public double bpm;
    public int beatSize;
    public int duration;
    public int barSize;
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
    private boolean metronomePlaying = false;
    public SimpleDiskCache mSimpleDiskCache;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Creating");
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        startAudio();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing");
        playing = false;
        mAudioTrack.pause();
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
        switch (id) {
            case R.id.action_comment:
                Intent loopIntent = new Intent(LoopActivity.this, CommentActivity.class);
                loopIntent.putExtra("trackId", Integer.parseInt(trackId));
                startActivity(loopIntent);
                break;
            case R.id.action_refresh:
                refreshLayout();
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
//        MenuItem metronome = menu.findItem(R.id.action_metronome);
//        metronome.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                if (metronomePlaying) {
//                    muteAudioData(metronomeData, 1);
//                } else {
//                    addAudioData(metronomeData, 1);
//                }
//                metronomePlaying = !metronomePlaying;
//                return true;
//            }
//        });
        return true;
    }

    public void init() {
        initCache();
        initVariables();
        setupLayouts();
        setupToolbar();
        initAudio();
        setupRestAdapter();
        getTrackInfo();
    }

    private void initCache() {
        LoopApplication loopApplication = (LoopApplication) getApplicationContext();
        mSimpleDiskCache = loopApplication.getSimpleDiskCache();
    }

    private void initVariables() {
        trackId = getIntent().getIntExtra("trackId", -1) + "";
        bpm = (double) getIntent().getIntExtra("BPM", 60);
        // basic samplerate, general for all devices.
        sampleRate = 44100;
        // the length of a beatSize in samples
        beatSize = (int) ((60.0 / bpm) * sampleRate);
        // the duration of a beat in milliseconds
        duration = (int) ((60.0 / bpm) * 1000);
        // a bar is one measure of 4/4 time
        barSize = beatSize * 4;
        // the max amount of bars. considered doing 16.
        maxBars = barSize * 8;
        // the initial blank array
        mAudioData = new short[barSize];
        try {
            metronomeData = generateMetronome();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private short[] generateMetronome() throws IOException {
        AssetManager assetManager = this.getAssets();
        InputStream tick = assetManager.open("tick.pcm");
        short[] tickData = new short[beatSize];
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(tick);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int j = 0;
            while (dataInputStream.available() > 0 && j < beatSize) {
                tickData[j] += (dataInputStream.readShort() * .5);
                j++;
            }

            dataInputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        short[] result = new short[barSize];
        int index = 0;
        for (int i = 0; i < barSize; i++) {
            if (index >= beatSize) {
                index = 0;
            }
            result[i] = tickData[index];
            index++;
        }
        return result;
    }

    private void setupLayouts() {
        mLeftLayout = (LinearLayout) findViewById(R.id.leftLayout);
        mRightLayout = (LinearLayout) findViewById(R.id.rightLayout);
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
                    mLoops.get(selected).getLoopButton().setSelected(false);
                    selected = -1;
                    materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
                    animateBottomToolbar();
                } else {
                    Log.d(TAG, "home selected");
                    LoopActivity.this.finish();
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
        if (bottomToolbarShowing && selected == -1) {
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
                    playbar.setSubtitle(mLoops.get(selected).getOwner());
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
        if (!playing) {
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
        if (token == "") {
            loginRedirect();
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
        Resources res = this.getResources();
        RestAdapter serverRestAdapter = new RestAdapter.Builder()
                .setEndpoint(res.getString(R.string.server_addr))
                .setRequestInterceptor(interceptor)
                .build();
        service = serverRestAdapter.create(ServerAPI.class);

        // setup s3 connection
        RestAdapter s3RestAdapter = new RestAdapter.Builder()
                .setEndpoint(res.getString(R.string.s3_addr))
                .build();
        s3Service = s3RestAdapter.create(S3API.class);
    }

    private void loginRedirect() {
        Dialog dialog = new Dialog(getApplicationContext(), "Error!", "You must be signed in!");
        dialog.show();
        Intent intent = new Intent(LoopActivity.this, LoginActivity.class);
        startActivity(intent);
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
        if (mLoopsLength < 6) {
            if (mLoopsLength % 2 == 0) {
                mLeftLayout.addView(addLoopObject());
            } else {
                mRightLayout.addView(addLoopObject());
            }
            mLoopsLength++;
        } else {
            Dialog dialog = new Dialog(LoopActivity.this, "Error!", "Max amount of loops reached!");
            dialog.show();
        }
    }

    public void deleteLoop(int id) {
        Loop loop = mLoops.get(id);
        if (loop.isPlaying()) {
            deleteAudioData(loop.getAudioData(), loop.getBars());
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
        for (Loop loop : mLoops) {
            loop.setIndex(index);
            ((LinearLayout) loop.getContainer().getParent()).removeView(loop.getContainer());
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
        loopButton.setId(mLoopsLength);

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());

        // Creating the container.
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                height);
        rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(rlp);


        int loopButtonDim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 130, getResources().getDisplayMetrics());
        // setting up the parameters to add the loop button
        RelativeLayout.LayoutParams loopParams = new RelativeLayout.LayoutParams(
                loopButtonDim,
                loopButtonDim);
        loopParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        // adding the actual button
        relativeLayout.addView(loopButton, loopParams);


        int loopDim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 110, getResources().getDisplayMetrics());
        // Adding the indeterminate progressbar
        ProgressBar progressBar = new ProgressBar(LoopActivity.this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(loopDim, loopDim);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(progressBar, params);

        // Adding the loop progressbar
        LoopProgressBar loopProgress = (LoopProgressBar) getLayoutInflater().inflate(R.layout.loop_circle_progress, null);
        loopProgress.setMax(1);
        loopProgress.setVisibility(View.INVISIBLE);
        loopProgress.setColor(primary_light);
        loopProgress.setDuration(duration);
        params = new RelativeLayout.LayoutParams(loopDim, loopDim);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(loopProgress, params);

        // Adding the loop to the global container
        mLoops.add(mLoopsLength, new Loop(relativeLayout, barSize));
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
            String endKey = endpoint.split("-")[0];
            addToLayout();
            Loop loop = mLoops.get(index);
            loop.setCurrentState("downloading");
            loop.setIndex(index);
            loop.setId(loops.get(index).id);
            loop.setEndpoint(endpoint);
            loop.setOwner(loops.get(index).User.name);
            SimpleDiskCache.InputStreamEntry inputStreamEntry = null;
            Log.d(TAG, "ENDPOINT TO BE FOUND.. " + endKey);
            try {
                inputStreamEntry = mSimpleDiskCache.getInputStream(endKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputStreamEntry != null) {
                Log.d(TAG, "PULLING FROM CACHE");
                StreamingTask task = new StreamingTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[]{inputStreamEntry.getInputStream(), index});
            } else {
                s3Service.getLoop(endpoint,
                        new Callback<Response>() {
                            @Override
                            public void success(Response result, Response response) {
                                if(result.getBody() != null) {
                                    Log.d(TAG, "SUCCESS INSIDE HERE");
                                    InputStream inputStream = null;
                                    try {
                                        inputStream = result.getBody().in();
                                        StreamingTask task = new StreamingTask();
                                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[]{inputStream, index});
                                    } catch (IOException e) {
                                        //e.printStackTrace();
                                    }
                                } else {
                                    Dialog dialog = new Dialog(LoopActivity.this, "Error", "Error loading loops");
                                    dialog.show();
                                    deleteFromServer(index);
                                }
                            }

                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.d(TAG, "Failed to download.");
                                retrofitError.printStackTrace();
                            }
                        });
            }
        }
    }

    /*
     *  GLOBAL AUDIO FUNCTIONS
     */

    private void setAudioData(short[] audioData) {
        mAudioData = audioData;
    }

    // Adds new audioData to the global mAudioData
    // PRE: takes in an array of pcm to add to the global array, and the bars it takes up
    private void addAudioData(short[] audioData, int bars) {
        int globalLength = barSize * totalBars;
        int localLength = barSize * bars;
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
            sum += audioData[localIndex];
            if (sum > Short.MAX_VALUE) {
                sum = Short.MAX_VALUE;
            } else if (sum < Short.MIN_VALUE) {
                sum = Short.MIN_VALUE;
            }
            result[index] = sum;
            globalIndex++;
            localIndex++;
            index++;
        }
        totalBars = Math.max(totalBars, bars);
        setAudioData(result);
    }

    // Removes audioData from the global mAudioData
    private void muteAudioData(short[] audioData, int bars) {
        int globalLength = barSize * totalBars;
        int localLength = barSize * bars;
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
            if (sum > Short.MAX_VALUE) {
                sum = Short.MAX_VALUE;
            } else if (sum < Short.MIN_VALUE) {
                sum = Short.MIN_VALUE;
            }
            result[index] = sum;
            globalIndex++;
            localIndex++;
            index++;
        }
        setAudioData(result);
    }

    private void deleteAudioData(short[] audioData, int localBars) {
        if(audioData != null) {
            findTotalBars();
            short[] globalAudioData = mAudioData;
            int globalLength = barSize * totalBars;
            int localLength = barSize * localBars;
            short[] result = new short[globalLength];
            int index = 0;
            int localIndex = 0;
            while (index < globalLength) {
                short sum = 0;
                if (localIndex >= localLength) {
                    localIndex = 0;
                }
                sum += globalAudioData[index];
                sum -= audioData[localIndex];
                result[index] = sum;
                index++;
                localIndex++;
            }
            setAudioData(result);
        }
    }

    public void findTotalBars() {
        int bars = 0;
        for (Loop loop : mLoops) {
            if (loop.isPlaying()) {
                bars = Math.max(bars, loop.getBars());
            }
        }
        totalBars = bars;
    }


    /*
     *  ONCLICK LISTENERS
     */

    // Initial recording OnClickListener
    public View.OnClickListener startRecOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int buttonId = v.getId();
            v.setOnClickListener(null);
            Log.d(TAG, "START REC");
            recording = true;
            recordFlag = true;
            RecordingTask task = new RecordingTask();
            task.execute(buttonId);
        }
    };

    // Stop the recording of the button.
    public View.OnClickListener stopRecOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "STOP REC");
            recording = false;
        }
    };

    // toggles individual audio tracks to play/not play
    public View.OnClickListener togglePlayOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Loop loop = mLoops.get(v.getId());
            if (loop.isPlaying() && selected != v.getId()) {
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
            if(selected != -1) {
                mLoops.get(selected).getLoopButton().setSelected(false);
            }
            if (selected == v.getId()) {
                selected = -1;
                materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
            } else {
                selected = v.getId();
                v.setSelected(true);
                materialMenu.animateIconState(MaterialMenuDrawable.IconState.X);
            }
            animateBottomToolbar();
            setupCancelEvent(findViewById(R.id.loopContainer));
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
            while (playing) {
                if (recordFlag == true) {
                    currentBeat = 0;
                    for (int i = 1; i <= 8; i++) {
                        if (i % 2 == 1) {
                            publishProgress(new Integer[]{-1, (i * - 1)});
                        }
                        mAudioTrack.write(metronomeData, ((beatSize / 2) * (i - 1)), (beatSize / 2));
                    }
                    currentBeat = 1;
                    recordFlag = false;
                }
                // totalBars is the number of bars. we are writing twice every
                // down beatSize for speed, so current beatSize is equivalent to 4 bars * 2
                if (currentBeat > (totalBars * 8)) {
                    currentBeat = 1;
                }
                // This is each time there is a down beatSize.
                if (currentBeat % 2 == 1) {
                    publishProgress(new Integer[]{1, (currentBeat / 2) + 1});
                }
                mAudioTrack.write(mAudioData, ((beatSize / 2) * (currentBeat - 1)), (beatSize / 2));
                currentBeat++;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... param) {
            if (param[0] == -1) {
                setLoopsProgress(param[1]);
            } else {
                setLoopsCountDown(param[1]);
            }
        }
    }

    private class RecordingTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            final int index = params[0];

            final File file = new File(Environment.getExternalStorageDirectory(), "test" + index + ".pcm");
            try {
                file.createNewFile();

                // sets up an outputstream which is a file
                OutputStream outputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                short[] tempAudioData = new short[minBufferSize];
                publishProgress(new Integer[]{0, index});
                while (recordFlag != false) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mAudioRecord.startRecording();
                publishProgress(new Integer[]{1, index});
                while (recording) {
                    int numberOfShort = mAudioRecord.read(tempAudioData, 0, minBufferSize);

                    for (int i = 0; i < numberOfShort; i++) {
                        dataOutputStream.writeShort(tempAudioData[i]);
                    }
                }

                mAudioRecord.stop();
                dataOutputStream.close();
                publishProgress(new Integer[]{-1, index});

                TypedFile soundFile = new TypedFile("binary", file);

                byte[] bytes = new byte[0];
                try {
                    InputStream inputStream = new FileInputStream(file);
                    bytes = IOUtils.toByteArray(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mLoops.get(index).setAudioDataFromByteArray(bytes);
                final byte[] byteCache = bytes;

                service.upload(soundFile, new Callback<Endpoint>() {
                    @Override
                    public void success(Endpoint data, Response response) {
                        if (data.type == true) {
                            Loop loop = mLoops.get(index);
                            loop.setEndpoint(data.endpoint);
                            loop.setId(data.id);
                            Log.d(TAG, "Loopid = " + loop.getId());
                            loop.setOwner(data.name);
                            InputStream is = new ByteArrayInputStream(byteCache);
                            try {
                                mSimpleDiskCache.put(data.endpoint, is);
                                Log.d(TAG, "PUTTING IN CACHE: " + data.endpoint);
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

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
            return index;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            if (params[0] == -1) {
                mLoops.get(params[1]).setCurrentState("uploading");
            } else if(params[0] == 0) {
                mLoops.get(params[1]).setCurrentState("ready");
            } else {
                mLoops.get(params[1]).setCurrentState("recording");
                mLoops.get(params[1]).getLoopButton().setOnClickListener(stopRecOnClickListener);
            }
        }

        @Override
        protected void onPostExecute(Integer index) {
            playPostExecute(index);
        }

    }

    private class StreamingTask extends AsyncTask<Object, Integer, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            final InputStream inputStream = (InputStream) params[0];
            final int index = (int) params[1];
            final String endpoint = mLoops.get(index).getEndpoint().split("-")[0];
            publishProgress(index);
            Loop loop = mLoops.get(index);
            byte[] bytes = new byte[0];
            try {
                bytes = IOUtils.toByteArray(inputStream);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            loop.setAudioDataFromByteArray(bytes);
            InputStream is = new ByteArrayInputStream(bytes);
            try {
                if (!mSimpleDiskCache.contains(endpoint)) {
                    mSimpleDiskCache.put(endpoint, is);
                    Log.d(TAG, "PUTTING IN CACHE: " + endpoint);
                }
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return index;
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            mLoops.get(params[0]).setCurrentState("downloading");
        }

        @Override
        protected void onPostExecute(Integer index) {
            playPostExecute(index);
        }
    }

    // after recording/streaming, it will add the audioData
    // and start playing it.
    public void playPostExecute(Integer index) {
        Loop loop = mLoops.get(index);
        LoopButton loopButton = loop.getLoopButton();
        addAudioData(loop.getAudioData(), loop.getBars());
        loopButton.setOnClickListener(togglePlayOnClickListener);
        loopButton.setOnLongClickListener(setLoopButtonSelected);
        loop.setCurrentState("playing");
    }

    public void setLoopsProgress(Integer value) {
        for (int i = 0; i < mLoopsLength; i++) {
            mLoops.get(i).setLoopProgress(value);
        }
    }

    public void setLoopsCountDown(Integer value) {
        for (int i = 0; i < mLoopsLength; i++) {
            mLoops.get(i).setLoopProgress(value);
        }
    }

    public void setupCancelEvent(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(selected != -1 && selected != v.getId()) {
                    mLoops.get(selected).getLoopButton().setSelected(false);
                    selected = -1;
                    materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
                    animateBottomToolbar();
                }
                return false;
            }
        });

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupCancelEvent(innerView);
            }
        }
    }
}