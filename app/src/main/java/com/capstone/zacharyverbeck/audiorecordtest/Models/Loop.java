package com.capstone.zacharyverbeck.audiorecordtest.Models;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;
import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopProgressBar;
import com.capstone.zacharyverbeck.audiorecordtest.R;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zacharyverbeck on 4/23/15.
 */
public class Loop {

    private RelativeLayout mContainer;
    private LoopButton mLoopButton;
    private ProgressBar mProgressBar;
    private LoopProgressBar mLoopProgressBar;
    private String filePath;
    private String endpoint;
    private int index;
    private int id;
    private short[] audioData;
    private boolean isPlaying;
    private String currentState;
    private int barSize;
    private int bars;

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
        this.setIndex(this.getLoopButton().getId());
        this.setFilePath(null);
        this.setAudioData(null);
        this.setIsPlaying(true);
        this.setCurrentState("ready");
        this.barSize = barSize;
        this.setBars(1);
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
        if(getCurrentState() == "loading" && state != "loading") {
            setProgressBarInvisible();
        }
        LoopButton button = this.getLoopButton();
        switch(state) {
            case "ready":
                button.setImageResource(R.drawable.ic_mic_none_white_48dp);
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
            case "loading":
                button.setImageResource(R.drawable.ic_file_download_48dp);
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.getLoopButton().setId(index);
        this.index = index;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        if(filePath != null) {
            setAudioData(getAudioDataFromFile(new File(filePath)));
        }
        this.filePath = filePath;
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
        if(audioData != null) {
            Log.d("LoopActivity", "Length = " + audioData.length);
            Log.d("LoopActivity", "Length = " + audioData.length % barSize);
            int bars = (audioData.length - (audioData.length % barSize));
            Log.d("LoopActivity", bars + " Bars b4 division");
            bars = bars / barSize;
            Log.d("LoopActivity", bars + " bars aftr division");
            if (bars <= 2) {
                setBars(bars);
            } else {
                setBars(Integer.highestOneBit(bars - 1));
            }
            Log.d("LoopActivity", this.bars + " many bars");

//            for(int i = 0; i < audioData.length; i+=1000) {
//                Log.d("LoopActivity", "OK FINE HERE: " + audioData[i] + " AT " + i);
//            }
        }
        this.audioData = audioData;
    }

    private short[] getAudioDataFromFile(File file) {
        int shortSizeInBytes = Short.SIZE / Byte.SIZE;
        int length = (int) (file.length() / shortSizeInBytes);
        if(length < barSize) {
            length = barSize;
        }
        short[] audioData = new short[length];
        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int j = 0;
            while (dataInputStream.available() > 0 && j < length) {
                audioData[j] += (dataInputStream.readShort() * .5);
                j++;
            }

            dataInputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return audioData;
    }


    public LoopProgressBar getLoopProgressBar() {
        return mLoopProgressBar;
    }

    public void setLoopProgressBar(LoopProgressBar loopProgressBar) {
        mLoopProgressBar = loopProgressBar;
    }

    public void setLoopProgress(int beat) {
        while(beat > bars) {
            beat = beat - bars;
        }
        if (beat == 1) {
            this.getLoopProgressBar().setProgress(Float.valueOf(0));
        }
        this.getLoopProgressBar().setProgressWithAnimation(Float.valueOf(beat));
    }

    public int getBars() {
        return bars;
    }

    public void setBars(int bars) {
        this.getLoopProgressBar().setMax(bars);
        this.bars = bars;
    }

}
