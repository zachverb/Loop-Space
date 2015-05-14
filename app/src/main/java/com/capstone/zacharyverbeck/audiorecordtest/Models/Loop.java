package com.capstone.zacharyverbeck.audiorecordtest.Models;

import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;

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

    private LoopButton mLoopButton;
    private String name;
    private String filePath;
    private String endpoint;
    private int id;
    private short[] audioData;

    public Loop(LoopButton loopButton) {
        this.setLoopButton(loopButton);
        this.setId(loopButton.getId());
        this.setName("ZGV");
        this.setFilePath(null);
        this.setAudioData(null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public LoopButton getLoopButton() {
        return mLoopButton;
    }

    public void setLoopButton(LoopButton loopButton) {
        mLoopButton = loopButton;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.audioData = audioData;
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
        return audioData;
    }


}
