package com.capstone.zacharyverbeck.loopspace.Models;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.capstone.zacharyverbeck.loopspace.Java.LoopButton;
import com.capstone.zacharyverbeck.loopspace.Java.LoopProgressBar;
import com.capstone.zacharyverbeck.loopspace.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by zacharyverbeck on 4/23/15.
 */
public class Loop implements Comparable<Loop> {

    private RelativeLayout mContainer;
    private LoopButton mLoopButton;
    private ProgressBar mProgressBar;
    private LoopProgressBar mLoopProgressBar;
    private String endpoint;
    private int id;
    private short[] audioData;
    private boolean isPlaying;
    private String currentState;
    private int barSize;
    private int bars;
    private String mOwner;

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public Loop(RelativeLayout layout, int barSize) {
        this.setContainer(layout);
        this.setLoopButton((LoopButton) layout.getChildAt(0));
        this.setProgressBar((ProgressBar) layout.getChildAt(1));
        this.setLoopProgressBar((LoopProgressBar) layout.getChildAt(2));
        this.setAudioData(null);
        this.setIsPlaying(true);
        this.setCurrentState("waiting");
        this.barSize = barSize;
        this.setBars(1);
    }

    private Animation getAnimation() {
        Animation animation = new AlphaAnimation(1f, .5f); // Change alpha from fully visible to invisible
        animation.setDuration(100);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(1);
        animation.setRepeatMode(Animation.REVERSE);
        return animation;
    }

    public LoopButton getLoopButton() {
        return mLoopButton;
    }

    public void setLoopButton(LoopButton loopButton) {
        mLoopButton = loopButton;
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        mProgressBar = progressBar;
    }

    public void setProgressBarVisible() {
        getProgressBar().setVisibility(View.VISIBLE);
    }

    public void setProgressBarInvisible() {
        getProgressBar().setVisibility(View.INVISIBLE);
    }

    public void setLoopProgressBarInvisible() {
        getLoopProgressBar().setVisibility(View.INVISIBLE);
    }

    public void setLoopProgressBarVisible() {
        getLoopProgressBar().setVisibility(View.VISIBLE);
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String state) {
        if ((getCurrentState() == "downloading" || getCurrentState() == "uploading") &&
                (state != "uploading" && state != "downloading")) {
            setProgressBarInvisible();
        }
        LoopButton button = this.getLoopButton();
        if(state != "ready") {
            button.setText("", 0f, Color.WHITE);
        }
        switch (state) {
            case "waiting":
                button.setImageResource(R.drawable.ic_mic_none_white_48dp);
                button.setColor(Color.parseColor("#2196F3"));
                break;
            case "ready":
                button.setImageResource(android.R.color.transparent);
                button.setColor(Color.parseColor("#2196F3"));
                break;
            case "recording":
                button.setImageResource(R.drawable.ic_mic_white_48dp);
                button.setColor(Color.RED);
                break;
            case "paused":
                button.setImageResource(R.drawable.ic_volume_off_white_48dp);
                button.setColor(Color.parseColor("#2196F3"));
                button.setAlpha(.5f);
                break;
            case "downloading":
                button.setImageResource(R.drawable.ic_file_download_48dp);
                button.setColor(Color.parseColor("#2196F3"));
                button.setAlpha(.5f);
                this.setProgressBarVisible();
                break;
            case "uploading":
                button.setImageResource(R.drawable.ic_file_upload_48dp);
                button.setColor(Color.parseColor("#2196F3"));
                button.setAlpha(.5f);
                this.setProgressBarVisible();
                break;
            case "playing":
                button.setImageResource(R.drawable.ic_play_arrow_blue_48dp);
                button.setColor(Color.parseColor("#2196F3"));
                button.setAlpha(1f);
                setLoopProgressBarVisible();
                break;
        }
        this.currentState = state;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RelativeLayout getContainer() {
        return mContainer;
    }

    public void setContainer(RelativeLayout container) {
        mContainer = container;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public short[] getAudioData() {
        return audioData;
    }

    public void setAudioData(short[] audioData) {
        if (audioData != null) {
            Log.d("LoopActivity", "Length = " + audioData.length);
            Log.d("LoopActivity", "Length = " + audioData.length % barSize);
            int bars = (audioData.length - (audioData.length % barSize));
            Log.d("LoopActivity", bars + " Bars b4 division");
            bars = bars / barSize;
            Log.d("LoopActivity", bars + " bars aftr division");
            if(bars < 1) {
                setBars(1);
            } else if (bars <= 2) {
                setBars(bars);
            } else {
                setBars(Integer.highestOneBit(bars));
            }
            Log.d("LoopActivity", this.bars + " many bars");
        }
        this.audioData = audioData;
    }

    public void setAudioDataFromByteArray(byte[] bytes) {
        int length = bytes.length/2;
        if(length < barSize) {
            length = barSize;
        }
        short[] shorts = new short[bytes.length/2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
        short[] audioData = new short[length];
        for(int i = 0; i < shorts.length; i++) {
            audioData[i] += shorts[i] * .5;
        }
        setAudioData(audioData);
    }

    public LoopProgressBar getLoopProgressBar() {
        return mLoopProgressBar;
    }

    public void setLoopProgressBar(LoopProgressBar loopProgressBar) {
        mLoopProgressBar = loopProgressBar;
    }

    public void setLoopProgress(int beat) {
        if(beat < 0) {
            if (this.currentState == "ready") {
                this.getLoopButton().setText(((5 - (-beat / 2 + 1)) + ""), 140f, Color.WHITE);
            }
            beat *= -1;
        } else {
            if (this.currentState == "ready") {
                this.getLoopButton().setText("", 0f, Color.WHITE);
            }
        }
        while (beat > (bars * 4)) {
            beat = beat - (bars * 4);
        }
        if (beat == 1) {
            this.getLoopProgressBar().setProgress(Float.valueOf(0));
        }
        this.getLoopProgressBar().setProgressWithAnimation(Float.valueOf(beat));
        if(this.currentState != "waiting" && this.currentState != "ready") {
            this.getLoopButton().startAnimation(getAnimation());
        }
    }

    public int getBars() {
        return bars;
    }

    public void setBars(int bars) {
        this.getLoopProgressBar().setMax(bars * 4);
        this.bars = bars;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public String getOwner() {
        return mOwner;
    }

    @Override
    public int compareTo(Loop another) {
        if(this.id > another.id) {
            return 1;
        } else if (this.id < another.id) {
            return -1;
        }
        return 0;
    }

}
